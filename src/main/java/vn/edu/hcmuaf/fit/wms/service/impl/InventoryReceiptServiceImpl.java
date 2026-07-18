package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.*;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.entity.enums.LpnStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.PutawayTaskStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.repository.*;
import vn.edu.hcmuaf.fit.wms.service.InventoryReceiptService;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReceiptServiceImpl implements InventoryReceiptService {

    private final InventoryReceiptRepository receiptRepository;
    private final InventoryStockService stockService;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;
    private final InventoryStockRepository stockRepository;
    private final LpnRepository lpnRepository;
    private final InventoryReceiptDetailRepository detailRepository;
    private final PutawayTaskRepository putawayTaskRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Page<ReceiptResponseDTO> getAllReceipts(String keyword, ReceiptStatus status, String assignedFilter,
            LocalDate fromDate, LocalDate toDate, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        String assignedTo = null;
        boolean unassigned = false;
        if ("ME".equalsIgnoreCase(assignedFilter)) {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                assignedTo = SecurityContextHolder.getContext().getAuthentication().getName();
            }
        } else if ("UNASSIGNED".equalsIgnoreCase(assignedFilter)) {
            unassigned = true;
        }

        Page<InventoryReceipt> receiptsPage = receiptRepository.searchInventoryReceipts(
                keyword, status, assignedTo, unassigned, fromDate, toDate, pageable);

        return receiptsPage.map(this::mapToDTO);
    }

    @Override
    public Page<ReceiptResponseDTO> getAvailableReceipts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return receiptRepository.findAvailableReceipts(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional
    public ReceiptResponseDTO claimReceipt(Long receiptId, String username) {
        // Use pessimistic write lock to prevent race condition
        InventoryReceipt receipt = receiptRepository.findByIdWithLock(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho!"));

        if (receipt.getStatus() != ReceiptStatus.RECEIVING) {
            throw new RuntimeException("Chỉ có thể nhận phiếu đang ở trạng thái RECEIVING! Trạng thái hiện tại: " + receipt.getStatus());
        }

        if (receipt.getAssignedTo() != null) {
            throw new RuntimeException("Phiếu này đã được nhân viên khác nhận: " + receipt.getAssignedTo());
        }

        receipt.setAssignedTo(username);
        return mapToDTO(receiptRepository.save(receipt));
    }

    @Override
    @Transactional
    public ReceiptResponseDTO createReceipt(ReceiptRequestDTO requestDTO) {
        // check supplier
        Partner supplier = partnerRepository.findById(requestDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà cung cấp!"));

        // get username
        String currentUser = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        // create Header
        InventoryReceipt receipt = InventoryReceipt.builder()
                .receiptCode("PNK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .supplier(supplier)
                .receiptDate(LocalDateTime.now())
                .status(ReceiptStatus.EXPECTED)
                .notes(requestDTO.getNotes())
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        // create Details
        List<InventoryReceiptDetail> details = requestDTO.getDetails().stream().map(dto -> {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            return InventoryReceiptDetail.builder()
                    .inventoryReceipt(receipt)
                    .product(product)
                    .location(null)
                    .quantity(dto.getQuantity())
                    .unitPrice(BigDecimal.valueOf(dto.getUnitPrice()))
                    .batchNo(dto.getBatchNo())
                    .expiryDate(parseLocalDate(dto.getExpiryDate()))
                    .serialNumber(dto.getSerialNumber())
                    .build();
        }).collect(Collectors.toList());

        receipt.setDetails(details);

        InventoryReceipt savedReceipt = receiptRepository.save(receipt);
        return mapToDTO(savedReceipt);
    }

    @Override
    @Transactional
    public ReceiptResponseDTO confirmReceipt(Long receiptId) {
        // find receipt
        InventoryReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho!"));

        // check status
        if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
            throw new RuntimeException("Phiếu nhập kho đã bị huỷ, không thể xác nhận!");
        }
        if (receipt.getStatus() != ReceiptStatus.EXPECTED) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu đang ở trạng thái EXPECTED! Trạng thái hiện tại: " + receipt.getStatus());
        }

        // check details
        if (receipt.getDetails() == null || receipt.getDetails().isEmpty()) {
            throw new RuntimeException("Không thể xác nhận phiếu nhập không có danh sách sản phẩm chi tiết!");
        }

        // preparing for Count & Label
        receipt.setStatus(ReceiptStatus.RECEIVING);

        InventoryReceipt savedReceipt = receiptRepository.save(receipt);
        return mapToDTO(savedReceipt);
    }

    @Override
    @Transactional
    public CountAndLabelResponseDTO countAndLabel(Long receiptId, Long detailId, CountAndLabelRequestDTO request) {

        // get receipt information
        InventoryReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        // check status
        if (receipt.getStatus() == ReceiptStatus.COMPLETED) {
            throw new RuntimeException("Phiếu nhập đã cất lên kệ hoàn tất, không thể kiểm đếm thêm");
        }
        if (receipt.getStatus() == ReceiptStatus.EXPECTED) {
            throw new RuntimeException("Phiếu nhập cần được xác nhận bởi quản lý thì mới có thể kiểm đếm");
        }

        // Check assigned employee
        String currentUser = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        if (receipt.getAssignedTo() != null && !receipt.getAssignedTo().equals(currentUser)) {
            throw new RuntimeException("Bạn không có quyền kiểm đếm phiếu này. Phiếu đã được nhận bởi: " + receipt.getAssignedTo());
        }

        // get receipt detail information
        InventoryReceiptDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết phiếu nhập"));

        // Check if the receipt detail belong to the receipt
        if (!detail.getInventoryReceipt().getId().equals(receiptId)) {
            throw new RuntimeException("Chi tiết không thuộc phiếu nhập này!");
        }

        // Update counted quantity
        Integer currentCounted = detail.getCountedQuantity() != null ? detail.getCountedQuantity() : 0;
        int newCounted = currentCounted + request.getCountedQuantity();
        if (newCounted > detail.getQuantity()) {
            throw new RuntimeException(
                    "Số lượng kiểm đếm (" + newCounted + ") vượt quá số lượng đặt hàng ("
                    + detail.getQuantity() + ") cho sản phẩm: " + detail.getProduct().getProductName()
            );
        }
        detail.setCountedQuantity(newCounted);
        detailRepository.save(detail);

        // Create new LPN
        String lpnCode = generateLpnCode();
        Lpn lpn = Lpn.builder()
                .lpnCode(lpnCode)
                .receipt(receipt)
                .receiptDetail(detail)
                .product(detail.getProduct())
                .quantity(request.getCountedQuantity())
                .batchNo(request.getBatchNo() != null ? request.getBatchNo() : detail.getBatchNo())
                .expiryDate(request.getExpiryDate() != null ? request.getExpiryDate() : detail.getExpiryDate())
                .status(LpnStatus.STAGED)
                .createdAt(LocalDateTime.now())
                .build();
        lpnRepository.save(lpn);

        // Create staging location
        StorageLocation stagingLocation = locationRepository.findFirstByLocationType(LocationType.RECEIVING_DOCK)
                .orElseThrow(() -> new RuntimeException("Lỗi: Chưa cấu hình vị trí Bãi nhận hàng (RECEIVING_DOCK) trong hệ thống!"));

        // Get Batch và Serial
        String currentBatchNo = lpn.getBatchNo();
        String currentSerialNumber = request.getSerialNumber();

        // check if there is a serial number
        if (currentSerialNumber != null && !currentSerialNumber.isEmpty()) {
            if (request.getCountedQuantity() != 1) {
                throw new RuntimeException("Lỗi: Sản phẩm quản lý theo Serial chỉ được phép nhập số lượng 1 cho mỗi lần quét!");
            }
        }

        // Add the counted quantity to the stage
        stockService.addStock(
                InventoryStockRequestDTO.builder()
                        .productId(detail.getProduct().getId())
                        .locationId(stagingLocation.getId())
                        .quantity(request.getCountedQuantity())
                        .batchNo(currentBatchNo)
                        .expiryDate(lpn.getExpiryDate())
                        .serialNumber(currentSerialNumber)
                        .build(),
                receipt.getReceiptCode()
        );

        // Check that all the details on the receipt have been counted
        InventoryReceipt freshReceipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));
        boolean isFullyCounted = freshReceipt.getDetails().stream()
                .allMatch(d -> d.getCountedQuantity() != null && d.getCountedQuantity() >= d.getQuantity());

        // When fully counted: move to PUTAWAY_PENDING and auto-create PutawayTask for each LPN
        if (isFullyCounted) {
            freshReceipt.setStatus(ReceiptStatus.PUTAWAY_PENDING);
            receiptRepository.save(freshReceipt);

            // Auto-create PutawayTask for each LPN of this receipt
            List<Lpn> allLpns = lpnRepository.findAllByReceipt_Id(receiptId);
            for (Lpn receiptLpn : allLpns) {
                PutawayTask task = PutawayTask.builder()
                        .receipt(freshReceipt)
                        .lpn(receiptLpn)
                        .product(receiptLpn.getProduct())
                        .status(PutawayTaskStatus.PENDING)
                        .build();
                putawayTaskRepository.save(task);
            }
        }

        // Create label printing (ZPL)
        String zplCommand = generateZplCommand(lpnCode, detail.getProduct().getProductName(), request.getCountedQuantity(), lpn.getBatchNo());

        return CountAndLabelResponseDTO.builder()
                .lpnCode(lpnCode)
                .zplCommand(zplCommand)
                .build();
    }

    @Override
    @Transactional
    public ReceiptResponseDTO createDraftReceipt(ReceiptRequestDTO requestDTO) {
        Partner supplier = partnerRepository.findById(requestDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà cung cấp!"));

        String currentUser = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        InventoryReceipt receipt = InventoryReceipt.builder()
                .receiptCode("PNK-DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .supplier(supplier)
                .receiptDate(LocalDateTime.now())
                .status(ReceiptStatus.DRAFT)
                .notes(requestDTO.getNotes())
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .scannedBy(currentUser)
                .scannedAt(LocalDateTime.now())
                .build();

        List<InventoryReceiptDetail> details = requestDTO.getDetails().stream().map(dto -> {
            // Trong DRAFT, productId có thể là null hoặc 0 (chưa khớp từ OCR)
            Product product = null;
            if (dto.getProductId() != null && dto.getProductId() > 0) {
                product = productRepository.findById(dto.getProductId())
                        .orElse(null); // Không throw – chỉ để null nếu không tìm thấy
            }

            return InventoryReceiptDetail.builder()
                    .inventoryReceipt(receipt)
                    .product(product)
                    .productNameRaw(dto.getProductNameRaw()) // Lưu tên OCR thô khi product chưa khớp
                    .location(null)
                    .quantity(dto.getQuantity() != null ? dto.getQuantity() : 1)
                    .unitPrice(dto.getUnitPrice() != null ? BigDecimal.valueOf(dto.getUnitPrice()) : BigDecimal.ZERO)
                    .batchNo(dto.getBatchNo())
                    .expiryDate(parseLocalDate(dto.getExpiryDate()))
                    .serialNumber(dto.getSerialNumber())
                    .build();
        }).collect(Collectors.toList());

        receipt.setDetails(details);

        InventoryReceipt savedReceipt = receiptRepository.save(receipt);

        try {
            messagingTemplate.convertAndSend("/topic/drafts", "new_draft");
        } catch (Exception e) {
            // Ignore broadcast failure
        }

        return mapToDTO(savedReceipt);
    }

    @Override
    @Transactional
    public ReceiptResponseDTO approveDraftReceipt(Long receiptId, ReceiptRequestDTO requestDTO) {
        InventoryReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nháp!"));

        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể duyệt phiếu đang ở trạng thái DRAFT! Trạng thái hiện tại: " + receipt.getStatus());
        }

        if (requestDTO.getDetails() == null || requestDTO.getDetails().isEmpty()) {
            throw new RuntimeException("Không thể duyệt phiếu nhập không có danh sách sản phẩm chi tiết!");
        }

        Partner supplier = partnerRepository.findById(requestDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà cung cấp!"));
        receipt.setSupplier(supplier);
        receipt.setNotes(requestDTO.getNotes());

        receipt.getDetails().clear();

        List<InventoryReceiptDetail> newDetails = requestDTO.getDetails().stream().map(dto -> {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            return InventoryReceiptDetail.builder()
                    .inventoryReceipt(receipt)
                    .product(product)
                    .location(null)
                    .quantity(dto.getQuantity())
                    .unitPrice(dto.getUnitPrice() != null ? BigDecimal.valueOf(dto.getUnitPrice()) : BigDecimal.ZERO)
                    .batchNo(dto.getBatchNo())
                    .expiryDate(parseLocalDate(dto.getExpiryDate()))
                    .serialNumber(dto.getSerialNumber())
                    .build();
        }).collect(Collectors.toList());

        receipt.getDetails().addAll(newDetails);

        receipt.setStatus(ReceiptStatus.RECEIVING);

        InventoryReceipt savedReceipt = receiptRepository.save(receipt);

        try {
            messagingTemplate.convertAndSend("/topic/drafts", "approved_draft");
        } catch (Exception e) {
            // Ignore broadcast failure
        }

        return mapToDTO(savedReceipt);
    }

    private ReceiptResponseDTO mapToDTO(InventoryReceipt entity) {
        if (entity == null) return null;

        // Calculate total amount
        BigDecimal totalAmount = entity.getDetails().stream()
                .map(d -> {
                    BigDecimal price = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal qty = BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 0);
                    return price.multiply(qty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReceiptResponseDTO.builder()
                .id(entity.getId())
                .receiptCode(entity.getReceiptCode())
                .notes(entity.getNotes())
                .supplierId(entity.getSupplier() != null ? entity.getSupplier().getId() : null)
                .supplierName(entity.getSupplier() != null ? entity.getSupplier().getName() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : entity.getReceiptDate())
                .createdBy(entity.getCreatedBy())
                .details(entity.getDetails().stream()
                        .map(detail -> {
                            BigDecimal price = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                            BigDecimal qty = BigDecimal.valueOf(detail.getQuantity() != null ? detail.getQuantity() : 0);

                            return ReceiptResponseDTO.ReceiptDetailResponseDTO.builder()
                                    .id(detail.getId())
                                    .productId(detail.getProduct() != null ? detail.getProduct().getId() : null)
                                    .productName(detail.getProduct() != null ? detail.getProduct().getProductName() : null)
                                    .productCode(detail.getProduct() != null ? detail.getProduct().getProductCode() : null)
                                    .productNameRaw(detail.getProductNameRaw()) // Tên OCR thô khi product chưa khớp
                                    .quantity(detail.getQuantity())
                                    .unitPrice(price)
                                    .totalPrice(price.multiply(qty))
                                    .locationName(detail.getLocation() != null ? detail.getLocation().getDescription() : null)
                                    .batchNo(detail.getBatchNo())
                                    .expiryDate(detail.getExpiryDate())
                                    .serialNumber(detail.getSerialNumber())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .totalAmount(totalAmount)
                .assignedTo(entity.getAssignedTo())
                .scannedBy(entity.getScannedBy())
                .scannedAt(entity.getScannedAt())
                .build();
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String cleaned = dateStr.trim();
        
        // 1. Try ISO format (yyyy-MM-dd)
        try {
            return LocalDate.parse(cleaned);
        } catch (Exception e) {}

        // 2. Try common formats
        String[] patterns = {
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "dd.MM.yyyy",
            "d/M/yyyy", "d-M-yyyy", "yyyy.MM.dd", "yyyy-M-d", "d/M/yy", "dd/MM/yy"
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {}
        }

        log.warn("Không thể parse ngày hết hạn: {}", dateStr);
        return null;
    }

    private String generateLpnCode() {
        return "LPN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private String generateZplCommand(String lpnCode, String productName, Integer qty, String batch) {
        String cleanName = productName.length() > 20 ? productName.substring(0, 20) : productName;
        String batchStr = batch != null ? batch : "N/A";

        return "^XA\n" +
                "^FO50,50^A0N,30,30^FDSP: " + cleanName + "^FS\n" +
                "^FO50,100^A0N,25,25^FDSLK: " + qty + " - Lo: " + batchStr + "^FS\n" +
                "^FO50,150^BY3^BCN,100,Y,N,N^FD" + lpnCode + "^FS\n" +
                "^XZ";
    }
}

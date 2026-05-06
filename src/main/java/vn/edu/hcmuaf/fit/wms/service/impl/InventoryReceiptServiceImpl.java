package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.*;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.entity.enums.LpnStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.repository.*;
import vn.edu.hcmuaf.fit.wms.service.InventoryReceiptService;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReceiptServiceImpl implements InventoryReceiptService {

    private final InventoryReceiptRepository receiptRepository;
    private final InventoryStockService stockService;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;
    private final InventoryStockRepository stockRepository;
    private final LpnRepository lpnRepository;
    private final InventoryReceiptDetailRepository detailRepository;

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
                    .expiryDate(dto.getExpiryDate())
                    .serialNumber(dto.getSerialNumber())
                    .build();
        }).collect(Collectors.toList());

        receipt.setDetails(details);

        InventoryReceipt savedReceipt = receiptRepository.save(receipt);
        return mapToDTO(savedReceipt);
    }

    @Override
    public List<ReceiptResponseDTO> getAllReceipts() {
        return receiptRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReceiptResponseDTO confirmReceipt(Long receiptId) {
        // find receipt
        InventoryReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho!"));

        // check status
        if (receipt.getStatus() != ReceiptStatus.EXPECTED) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu đang ở trạng thái EXPECTED!");
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

        // get receipt detail information
        InventoryReceiptDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết phiếu nhập"));

        // Update counted quantity
        Integer currentCounted = detail.getCountedQuantity() != null ? detail.getCountedQuantity() : 0;
        detail.setCountedQuantity(currentCounted + request.getCountedQuantity());
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
                .build();
        lpnRepository.save(lpn);

        // Create staging location
        StorageLocation stagingLocation = locationRepository.findFirstByLocationType(LocationType.RECEIVING_DOCK)
                .orElseThrow(() -> new RuntimeException("Lỗi: Chưa cấu hình vị trí Bãi nhận hàng (RECEIVING_DOCK) trong hệ thống!"));

        // Get Batch và Serial
        String currentBatchNo = lpn.getBatchNo();
        String currentSerialNumber = request.getSerialNumber();

        // Check if this product is already available at the stage
        InventoryStock stagingStock = stockRepository
                .findByProductAndLocationAndBatchNoAndSerialNumber(detail.getProduct(), stagingLocation, currentBatchNo, currentSerialNumber)
                .orElseGet(() -> InventoryStock.builder()
                        .product(detail.getProduct())
                        .location(stagingLocation)
                        .quantity(0)
                        .batchNo(currentBatchNo)
                        .expiryDate(lpn.getExpiryDate())
                        .serialNumber(currentSerialNumber)
                        .build());

        // check if there is a serial number
        if (currentSerialNumber != null && !currentSerialNumber.isEmpty()) {
            if (request.getCountedQuantity() != 1) {
                throw new RuntimeException("Lỗi: Sản phẩm quản lý theo Serial chỉ được phép nhập số lượng 1 cho mỗi lần quét!");
            }
            if (stagingStock.getQuantity() > 0) {
                throw new RuntimeException("Lỗi: Số Serial " + currentSerialNumber + " này đã được kiểm đếm hoặc đã tồn tại trong hệ thống!");
            }
        }

        // Add the counted quantity to the stage
        stagingStock.setQuantity(stagingStock.getQuantity() + request.getCountedQuantity());
        stockRepository.save(stagingStock);

        // Check that all the details on the receipt have been counted
        boolean isFullyCounted = receipt.getDetails().stream()
                .allMatch(d -> d.getCountedQuantity() != null && d.getCountedQuantity() >= d.getQuantity());

        // preparing for Putaway
        if (isFullyCounted) {
            receipt.setStatus(ReceiptStatus.PUTAWAY_PENDING);
            receiptRepository.save(receipt);
        }

        // Create label printing (ZPL)
        String zplCommand = generateZplCommand(lpnCode, detail.getProduct().getProductName(), request.getCountedQuantity(), lpn.getBatchNo());

        return CountAndLabelResponseDTO.builder()
                .lpnCode(lpnCode)
                .zplCommand(zplCommand)
                .build();
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
                .build();
    }

    private String generateLpnCode() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);

        long countToday = lpnRepository.countByCreatedAtBetween(startOfDay, endOfDay) + 1;
        String dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));

        return String.format("LPN-%s-%04d", dateStr, countToday);
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

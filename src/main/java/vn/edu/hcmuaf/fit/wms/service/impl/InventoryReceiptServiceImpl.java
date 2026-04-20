package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.repository.InventoryReceiptRepository;
import vn.edu.hcmuaf.fit.wms.repository.PartnerRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
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

    @Override
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
                .status(ReceiptStatus.PENDING)
                .notes(requestDTO.getNotes())
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        // create Details
        List<InventoryReceiptDetail> details = requestDTO.getDetails().stream().map(dto -> {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            StorageLocation location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Vị trí không tồn tại"));

            return InventoryReceiptDetail.builder()
                    .inventoryReceipt(receipt)
                    .product(product)
                    .location(location)
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
    public ReceiptResponseDTO confirmReceipt(Long receiptId) {
        // find receipt
        InventoryReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho!"));

        // check status
        if (receipt.getStatus() != ReceiptStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu đang ở trạng thái PENDING!");
        }

        // review each detail on the invoice to add to inventory
        for (InventoryReceiptDetail detail : receipt.getDetails()) {
            stockService.addStock(InventoryStockRequestDTO.builder()
                            .productId(detail.getProduct().getId())
                            .locationId(detail.getLocation().getId())
                            .quantity(detail.getQuantity())
                            .batchNo(detail.getBatchNo())
                            .expiryDate(detail.getExpiryDate())
                            .serialNumber(detail.getSerialNumber())
                    .build(),
                    receipt.getReceiptCode()
            );
        }

        receipt.setStatus(ReceiptStatus.COMPLETED);

        InventoryReceipt savedReceipt = receiptRepository.save(receipt);
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
}

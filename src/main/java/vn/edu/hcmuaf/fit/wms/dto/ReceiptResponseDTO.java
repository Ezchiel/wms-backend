package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ReceiptResponseDTO {
    private Long id;
    private String receiptCode;
    private Long supplierId;
    private String supplierName;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<ReceiptDetailResponseDTO> details;
    private BigDecimal totalAmount;
    private String assignedTo;

    @Getter
    @Setter
    @Builder
    public static class ReceiptDetailResponseDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String locationName;
        private BigDecimal totalPrice;
        private String batchNo;
        private LocalDate expiryDate;
        private String serialNumber;
    }
}

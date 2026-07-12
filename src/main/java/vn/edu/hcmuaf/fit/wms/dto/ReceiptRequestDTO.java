package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReceiptRequestDTO {
    private Long supplierId;
    private String notes;
    private List<ReceiptDetailDTO> details;

    @Getter
    @Setter
    public static class ReceiptDetailDTO {
        private Long productId;
        private String productNameRaw; // Tên sản phẩm thô từ OCR (khi productId chưa khớp)
        private Integer quantity;
        private Double unitPrice;
        private String batchNo;
        private String expiryDate;
        private String serialNumber;
    }
}

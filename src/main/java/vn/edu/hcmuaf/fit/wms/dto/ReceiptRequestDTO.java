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
        private Long locationId;
        private Integer quantity;
        private Double unitPrice;
        private String batchNo;
        private LocalDate expiryDate;
        private String serialNumber;
    }
}

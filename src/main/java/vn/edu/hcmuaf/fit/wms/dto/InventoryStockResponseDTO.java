package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class InventoryStockResponseDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Long locationId;
    private Integer quantity;
    private String batchNo;
    private LocalDate expiryDate;
    private String serialNumber;
}
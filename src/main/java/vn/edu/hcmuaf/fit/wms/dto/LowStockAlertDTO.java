package vn.edu.hcmuaf.fit.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDTO {
    private Long productId;
    private String productCode;
    private String productName;
    private Integer minStockLevel;
    private Long currentTotalStock;
}

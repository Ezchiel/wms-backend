package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequestDTO {
    private String productCode;
    private String productName;
    private String unit;
    private Long groupId;
    private String description;
}

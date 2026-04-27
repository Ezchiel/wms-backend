package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountAndLabelResponseDTO {
    private String lpnCode;
    private String productName;
    private Integer quantity;
    private String zplCommand;
}
package vn.edu.hcmuaf.fit.wms.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PutawaySuggestionDTO {
    private String lpnCode;
    private String productName;
    private String suggestedLocationCode;
    private Long suggestedLocationId;
    private Integer availableCapacity;
    private String unit;  // Đơn vị tính của sản phẩm (vd: thùng, chai, cái)
}
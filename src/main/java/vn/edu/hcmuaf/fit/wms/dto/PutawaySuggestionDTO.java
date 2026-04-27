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
}
package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CheckResponseDTO {
    private String checkCode;
    private LocalDateTime checkDate;
    private CheckStatus status;
    private String notes;
    private List<CheckDetailResponseDTO> details;

    @Getter
    @Setter
    @Builder
    public static class CheckDetailResponseDTO {
        private Long productId;
        private Long locationId;
        private Integer systemQuantity;
        private Integer actualQuantity;
        private Integer variance;
        private String reason;
    }
}

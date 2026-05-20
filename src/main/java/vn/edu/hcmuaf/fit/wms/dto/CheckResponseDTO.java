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
    private Long id;
    private String checkCode;
    private LocalDateTime checkDate;
    private CheckStatus status;
    private String notes;
    private String createdBy;
    private List<CheckDetailResponseDTO> details;

    @Getter
    @Setter
    @Builder
    public static class CheckDetailResponseDTO {
        private Long id;
        private Long productId;
        private String productName;
        private Long locationId;
        private String locationBarcode;
        private String batchNo;
        private Integer systemQuantity;
        private Integer actualQuantity;
        private Integer variance;
        private String reason;
    }
}

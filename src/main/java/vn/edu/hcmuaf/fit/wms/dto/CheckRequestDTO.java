package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckRequestDTO {
    private String notes;
    private List<CheckDetailDTO> details;

    @Getter
    @Setter
    public static class CheckDetailDTO {
        private Long productId;
        private Long locationId;
        private String batchNo;
        private Integer actualQuantity;
        private String reason;
    }
}

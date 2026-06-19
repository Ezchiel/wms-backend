package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IssueRequestDTO {
    private Long customerId;
    private String notes;
    private List<IssueDetailDTO> details;

    @Getter
    @Setter
    public static class IssueDetailDTO {
        private Long productId;
        private Long locationId;
        private Integer quantity;
        private String batchNo;
    }
}

package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class IssueResponseDTO {
    private Long id;
    private String issueCode;
    private Long customerId;
    private String customerName;
    private LocalDateTime issueDate;
    private IssueStatus status;
    private String notes;
    private String assignedTo;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<IssueDetailResponseDTO> details;

    @Getter
    @Setter
    @Builder
    public static class IssueDetailResponseDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productCode;
        private Long locationId;
        private String locationBarcode;
        private String locationDescription;
        private Integer quantity;
    }
}

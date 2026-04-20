package vn.edu.hcmuaf.fit.wms.dto;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class IssueResponseDTO {
    private String issueCode;
    private String customerName;
    private LocalDateTime issueDate;
    private ReceiptStatus status;
    private String notes;
    private List<IssueDetailResponseDTO> details;

    @Getter
    @Setter
    @Builder
    public static class IssueDetailResponseDTO {
        private String productName;
        private String locationDescription;
        private Integer quantity;
    }
}

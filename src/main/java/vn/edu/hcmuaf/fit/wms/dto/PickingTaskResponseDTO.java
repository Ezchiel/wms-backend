package vn.edu.hcmuaf.fit.wms.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickingTaskResponseDTO {
    private Long id;
    private Long issueId;
    private String issueCode;
    private Long productId;
    private String productName;
    private String productCode;
    private Long locationId;
    private String locationBarcode;
    private String locationDescription;
    private Integer requiredQuantity;
    private Integer pickedQuantity;
    private PickingTaskStatus status;
    private String assignedTo;
    private String note;
    private LocalDateTime pickedAt;
    private LocalDateTime createdAt;
}

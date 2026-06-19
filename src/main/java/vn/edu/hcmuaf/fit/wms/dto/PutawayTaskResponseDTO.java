package vn.edu.hcmuaf.fit.wms.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.PutawayTaskStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PutawayTaskResponseDTO {
    private Long id;
    private Long receiptId;
    private String receiptCode;
    private String lpnCode;
    private Long productId;
    private String productName;
    private Integer quantity;
    private String batchNo;
    private PutawayTaskStatus status;
    private String assignedTo;
    private String suggestedLocationCode;
    private Long suggestedLocationId;
    private Long targetLocationId;
    private String note;
    private LocalDateTime putawayAt;
    private LocalDateTime createdAt;
}

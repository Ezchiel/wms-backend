package vn.edu.hcmuaf.fit.wms.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PutawayTaskConfirmRequestDTO {
    private Long taskId;
    private Long locationId;
    private String note;
}

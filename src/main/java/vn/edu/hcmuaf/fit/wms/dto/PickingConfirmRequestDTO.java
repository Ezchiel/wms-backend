package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PickingConfirmRequestDTO {
    private Long taskId;
    private Integer pickedQuantity;
    private String note;
}

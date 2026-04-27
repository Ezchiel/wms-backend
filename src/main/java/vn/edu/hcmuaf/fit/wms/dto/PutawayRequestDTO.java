package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PutawayRequestDTO {
    private String lpnCode;
    private Long locationId;
}
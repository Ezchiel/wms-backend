package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CountAndLabelRequestDTO {
    private Integer countedQuantity;
    private String batchNo;
    private LocalDate expiryDate;
}
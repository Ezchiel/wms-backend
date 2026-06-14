package vn.edu.hcmuaf.fit.wms.dto;

import java.math.BigDecimal;

public record StockByGroupDTO(
        Long groupId,
        String groupCode,
        String groupName,
        Long totalQuantity,
        BigDecimal totalValue
) {}

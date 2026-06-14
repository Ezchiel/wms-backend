package vn.edu.hcmuaf.fit.wms.dto;

import java.time.LocalDate;

public record ExpiringStockDTO(
        Long stockId,
        Long productId,
        String productCode,
        String productName,
        Long locationId,
        String locationBarcode,
        String batchNo,
        LocalDate expiryDate,
        long daysRemaining,
        Integer quantity
) {}

package vn.edu.hcmuaf.fit.wms.dto;

public record InventoryMovementDTO(
        Long productId,
        String productCode,
        String productName,
        long openingStock,
        long totalReceipt,
        long totalIssue,
        long totalAdjust,
        long closingStock
) {}

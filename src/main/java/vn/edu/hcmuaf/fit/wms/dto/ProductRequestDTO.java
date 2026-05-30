package vn.edu.hcmuaf.fit.wms.dto;

public record ProductRequestDTO(
    String productCode,
    String productName,
    String unit,
    Long groupId,
    String description,
    Integer minStockLevel
) {}

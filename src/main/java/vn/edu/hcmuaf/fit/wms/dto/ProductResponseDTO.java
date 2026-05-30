package vn.edu.hcmuaf.fit.wms.dto;

import vn.edu.hcmuaf.fit.wms.entity.Product;

public record ProductResponseDTO(
    Long id,
    String productCode,
    String productName,
    String unit,
    Long groupId,
    String groupCode,
    String groupName,
    String description,
    Integer minStockLevel,
    String batchNo
) {
    /** Dùng cho tra cứu thông thường – không gắn lô hàng cụ thể */
    public static ProductResponseDTO fromEntity(Product product) {
        if (product == null) return null;
        return new ProductResponseDTO(
            product.getId(),
            product.getProductCode(),
            product.getProductName(),
            product.getUnit(),
            product.getProductGroup() != null ? product.getProductGroup().getId() : null,
            product.getProductGroup() != null ? product.getProductGroup().getGroupCode() : null,
            product.getProductGroup() != null ? product.getProductGroup().getGroupName() : null,
            product.getDescription(),
            product.getMinStockLevel(),
            null
        );
    }

    /** Dùng khi tra cứu qua LPN – batchNo được lấy từ bản ghi LPN */
    public static ProductResponseDTO fromEntityWithBatch(Product product, String batchNo) {
        if (product == null) return null;
        return new ProductResponseDTO(
            product.getId(),
            product.getProductCode(),
            product.getProductName(),
            product.getUnit(),
            product.getProductGroup() != null ? product.getProductGroup().getId() : null,
            product.getProductGroup() != null ? product.getProductGroup().getGroupCode() : null,
            product.getProductGroup() != null ? product.getProductGroup().getGroupName() : null,
            product.getDescription(),
            product.getMinStockLevel(),
            batchNo
        );
    }
}


package vn.edu.hcmuaf.fit.wms.dto;

import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;

public record StorageLocationResponseDTO(
    Long id,
    String zone,
    String rack,
    String shelf,
    String barcode,
    String description,
    Integer maxCapacity,
    int currentQuantity,
    Integer availableCapacity,
    boolean isFull,
    double fillRate,
    Integer pathSequence,
    LocationType locationType,
    // Thông tin sản phẩm đang khóa vị trí (null nếu vị trí trống)
    Long lockedProductId,
    String lockedProductName,
    String unit   // Đơn vị tính của sản phẩm đang khóa vị trí
) {
    /** Backward-compatible: dùng khi không cần thông tin sản phẩm khóa (vị trí trống hoặc chưa tra cứu). */
    public static StorageLocationResponseDTO fromEntity(StorageLocation location) {
        return fromEntity(location, 0, null, null, null);
    }

    /** Dùng khi chỉ cần currentQuantity, chưa có thông tin sản phẩm khóa. */
    public static StorageLocationResponseDTO fromEntity(StorageLocation location, int currentQuantity) {
        return fromEntity(location, currentQuantity, null, null, null);
    }

    /** Full version: truyền đầy đủ thông tin sản phẩm đang khóa vị trí. */
    public static StorageLocationResponseDTO fromEntity(StorageLocation location, int currentQuantity,
                                                        Long lockedProductId, String lockedProductName,
                                                        String unit) {
        if (location == null) return null;
        Integer max = location.getMaxCapacity();
        Integer available = max != null ? max - currentQuantity : null;
        boolean full = max != null && currentQuantity >= max;
        double fillRate = max == null || max <= 0 ? 0.0
                : Math.round(((double) currentQuantity / max) * 1000.0) / 1000.0;

        return new StorageLocationResponseDTO(
            location.getId(),
            location.getZone(),
            location.getRack(),
            location.getShelf(),
            location.getBarcode(),
            location.getDescription(),
            max,
            currentQuantity,
            available,
            full,
            fillRate,
            location.getPathSequence(),
            location.getLocationType(),
            lockedProductId,
            lockedProductName,
            unit
        );
    }
}

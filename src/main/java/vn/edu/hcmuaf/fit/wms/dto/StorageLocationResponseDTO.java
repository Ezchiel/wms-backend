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
    boolean isFull,
    Integer pathSequence,
    LocationType locationType
) {
    public static StorageLocationResponseDTO fromEntity(StorageLocation location) {
        if (location == null) return null;
        return new StorageLocationResponseDTO(
            location.getId(),
            location.getZone(),
            location.getRack(),
            location.getShelf(),
            location.getBarcode(),
            location.getDescription(),
            location.isFull(),
            location.getPathSequence(),
            location.getLocationType()
        );
    }
}

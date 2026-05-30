package vn.edu.hcmuaf.fit.wms.dto;

import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;

public record StorageLocationRequestDTO(
    String zone,
    String rack,
    String shelf,
    String barcode,
    String description,
    boolean isFull,
    Integer pathSequence,
    LocationType locationType
) {
    public StorageLocation toEntity() {
        return StorageLocation.builder()
                .zone(this.zone)
                .rack(this.rack)
                .shelf(this.shelf)
                .barcode(this.barcode)
                .description(this.description)
                .isFull(this.isFull)
                .pathSequence(this.pathSequence)
                .locationType(this.locationType != null ? this.locationType : LocationType.STORAGE)
                .build();
    }
}

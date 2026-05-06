package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;

@Entity
@Table(name = "storage_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String zone;

    @Column(nullable = false)
    private String rack;

    @Column(nullable = false)
    private String shelf;

    @Column(unique = true, nullable = false)
    private String barcode;

    private String description;

    @Column(name = "is_full")
    private boolean isFull = false;

    @Column(name = "path_sequence")
    private Integer pathSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private LocationType locationType = LocationType.STORAGE;
}

package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_check_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id", nullable = false)
    private InventoryCheck inventoryCheck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(nullable = false)
    private Integer variance;

    private String reason;
}

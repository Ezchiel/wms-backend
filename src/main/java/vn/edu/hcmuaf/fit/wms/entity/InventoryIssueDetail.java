package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_issue_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryIssueDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private InventoryIssue inventoryIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private StorageLocation location;

    @Column(nullable = false)
    private Integer quantity;
}

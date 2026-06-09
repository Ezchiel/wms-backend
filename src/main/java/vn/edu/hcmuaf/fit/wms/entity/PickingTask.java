package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "picking_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private InventoryIssue inventoryIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_detail_id", nullable = false)
    private InventoryIssueDetail issueDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;

    @Column(name = "picked_quantity")
    @Builder.Default
    private Integer pickedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PickingTaskStatus status = PickingTaskStatus.PENDING;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "note")
    private String note;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.pickedQuantity == null) {
            this.pickedQuantity = 0;
        }
        if (this.status == null) {
            this.status = PickingTaskStatus.PENDING;
        }
    }
}

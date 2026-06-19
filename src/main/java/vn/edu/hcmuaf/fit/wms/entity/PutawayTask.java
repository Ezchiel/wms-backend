package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.PutawayTaskStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "putaway_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PutawayTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private InventoryReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lpn_id", nullable = false)
    private Lpn lpn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PutawayTaskStatus status;

    @Column(name = "target_location_id")
    private Long targetLocationId;

    private String note;

    @Column(name = "putaway_at")
    private LocalDateTime putawayAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

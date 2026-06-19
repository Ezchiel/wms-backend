package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inventory_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", unique = true, nullable = false)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Partner supplier;

    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    private String notes;

    @OneToMany(mappedBy = "inventoryReceipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryReceiptDetail> details;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "assigned_to")
    private String assignedTo;
}


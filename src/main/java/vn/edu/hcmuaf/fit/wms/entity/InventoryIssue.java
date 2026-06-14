package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inventory_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_code", unique = true, nullable = false)
    private String issueCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Partner customer;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Enumerated(EnumType.STRING)
    private IssueStatus status;

    private String notes;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "inventoryIssue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryIssueDetail> details;
}

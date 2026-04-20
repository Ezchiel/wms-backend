package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inventory_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_code", unique = true, nullable = false)
    private String checkCode;

    @Column(name = "check_date")
    private LocalDateTime checkDate;

    @Enumerated(EnumType.STRING)
    private CheckStatus status;

    private String notes;

    @OneToMany(mappedBy = "inventoryCheck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryCheckDetail> details;
}

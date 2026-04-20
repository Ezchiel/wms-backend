package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;

@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerType type;

    private String phone;

    private String email;

    private String address;

    @Column(name = "tax_code")
    private String taxCode;
}

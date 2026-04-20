package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.Role;
import vn.edu.hcmuaf.fit.wms.entity.enums.UserStatus;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    private String password;
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserStatus status;
}

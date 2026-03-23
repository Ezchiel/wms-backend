package vn.edu.hcmuaf.fit.wms.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.UserStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Integer id;
    private String username;
    private String fullName;
    private String roleName;
    private String email;
    private String phone;
    private UserStatus status;
}

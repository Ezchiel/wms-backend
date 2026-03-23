package vn.edu.hcmuaf.fit.wms.dto;

import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.UserStatus;

@Getter
@Setter
public class UserRequestDTO {
    private String username;
    private String password;
    private String fullName;
    private Integer roleId;
    private String email;
    private String phone;
    private UserStatus status;
}

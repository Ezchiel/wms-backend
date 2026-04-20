package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.UserRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.UserResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.Role;
import vn.edu.hcmuaf.fit.wms.entity.enums.UserStatus;

public interface UserService {
    Page<UserResponseDTO> getAllUsers(String keyword, UserStatus status, Role role,
                                      int page, int size, String sortBy, String sortDir);
    UserResponseDTO getUserById(Integer id);
    UserResponseDTO createUser(UserRequestDTO requestDTO);
    UserResponseDTO updateUser(Integer id, UserRequestDTO requestDTO);
    void deleteUser(Integer id);
    void restoreUser(Integer id);
    void lockUser(Integer id);
    void unlockUser(Integer id);
}

package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.UserRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.UserResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.User;

import java.util.List;

public interface UserService {
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO getUserById(Integer id);
    UserResponseDTO createUser(UserRequestDTO requestDTO);
    UserResponseDTO updateUser(Integer id, UserRequestDTO requestDTO);
    void deleteUser(Integer id);
}

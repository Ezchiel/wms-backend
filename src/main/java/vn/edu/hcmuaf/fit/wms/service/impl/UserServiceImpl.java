package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.UserRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.UserResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.Role;
import vn.edu.hcmuaf.fit.wms.entity.User;
import vn.edu.hcmuaf.fit.wms.entity.UserStatus;
import vn.edu.hcmuaf.fit.wms.repository.RoleRepository;
import vn.edu.hcmuaf.fit.wms.repository.UserRepository;
import vn.edu.hcmuaf.fit.wms.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan voi ID: " + id));
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        // check duplicate
        if (userRepository.existsByUsername(requestDTO.getUsername())) {
            throw new RuntimeException("Ten dang nhap da ton tai!");
        }

        // map from DTO to User
        User user = new User();
        user.setUsername(requestDTO.getUsername());
        user.setFullName(requestDTO.getFullName());
        user.setEmail(requestDTO.getEmail());
        user.setPhone(requestDTO.getPhone());
        user.setStatus(UserStatus.ACTIVE);

        // handle DTO's role
        if (requestDTO.getRoleId() != null) {
            Role role = roleRepository.findById(requestDTO.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay quyen (Role) nay!"));
            user.setRole(role);
        }

        // password encoder
        String encodedPassword = passwordEncoder.encode(requestDTO.getPassword());
        user.setPassword(encodedPassword);

        User savedUser = userRepository.save(user);
        return mapToResponseDTO(savedUser);
    }

    @Override
    public UserResponseDTO updateUser(Integer id, UserRequestDTO requestDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan voi ID: " + id));

        existingUser.setFullName(requestDTO.getFullName());
        existingUser.setEmail(requestDTO.getEmail());
        existingUser.setPhone(requestDTO.getPhone());

        // update role
        if (requestDTO.getRoleId() != null) {
            Role role = roleRepository.findById(requestDTO.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay quyen (Role) nay!"));
            existingUser.setRole(role);
        }

        // check if there is a new password
        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(requestDTO.getPassword());
            existingUser.setPassword(encodedPassword);
        }

        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDTO(updatedUser);
    }

    @Override
    public void deleteUser(Integer id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan voi ID: " + id));
        userRepository.delete(existingUser);
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .build();
    }
}

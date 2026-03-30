package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.UserRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.UserResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.Role;
import vn.edu.hcmuaf.fit.wms.entity.User;
import vn.edu.hcmuaf.fit.wms.entity.UserStatus;
import vn.edu.hcmuaf.fit.wms.repository.UserRepository;
import vn.edu.hcmuaf.fit.wms.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserResponseDTO> getAllUsers(String keyword, UserStatus status, Role role,
                                             int page, int size, String sortBy, String sortDir) {
        // configure sort (ASC or DESC)
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<User> usersPage = userRepository.searchAndFilterUsers(keyword, status, role, pageable);

        return usersPage.map(this::mapToResponseDTO);
    }

    @Override
    public UserResponseDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        User currentUser = getCurrentLoggedInUser();

        // check if the current user has permission to create this role
        validateRoleAssignment(currentUser, requestDTO.getRole());

        // check duplicate username
        if (userRepository.existsByUsername(requestDTO.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        // check duplicate email
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // map from DTO to User
        User user = new User();
        user.setUsername(requestDTO.getUsername());
        user.setFullName(requestDTO.getFullName());
        user.setEmail(requestDTO.getEmail());
        user.setPhone(requestDTO.getPhone());
        user.setStatus(UserStatus.ACTIVE);

        // handle DTO's role
        if (requestDTO.getRole() != null) {
            user.setRole(requestDTO.getRole());
        }

        // password encoder
        String encodedPassword = passwordEncoder.encode(requestDTO.getPassword());
        user.setPassword(encodedPassword);

        User savedUser = userRepository.save(user);
        return mapToResponseDTO(savedUser);
    }

    @Override
    public UserResponseDTO updateUser(Integer id, UserRequestDTO requestDTO) {
        // unable to update status or role of Root Admin
        if (id == 1) {
            if (requestDTO.getStatus() != null && requestDTO.getStatus() != UserStatus.ACTIVE) {
                throw new RuntimeException("Không thể thay đổi trạng thái của Admin gốc!");
            }
            if (requestDTO.getRole() != null && requestDTO.getRole() != Role.ADMIN) {
                throw new RuntimeException("Không thể hạ quyền của Admin gốc!");
            }
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        User currentUser = getCurrentLoggedInUser();

        validatePermission(currentUser, targetUser);

        // check if there is an email updated
        if (!targetUser.getEmail().equals(requestDTO.getEmail()) && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        targetUser.setUsername(requestDTO.getUsername());
        targetUser.setFullName(requestDTO.getFullName());
        targetUser.setEmail(requestDTO.getEmail());
        targetUser.setPhone(requestDTO.getPhone());

        // update role
        if (requestDTO.getRole() != null) {
            targetUser.setRole(requestDTO.getRole());
        }

        // update status
        if (requestDTO.getStatus() != null) {
            targetUser.setStatus(requestDTO.getStatus());
        }

        // check if there is a new password
        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(requestDTO.getPassword());
            targetUser.setPassword(encodedPassword);
        }

        User updatedUser = userRepository.save(targetUser);
        return mapToResponseDTO(updatedUser);
    }

    @Override
    public void deleteUser(Integer id) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        User currentUser = getCurrentLoggedInUser();

        validatePermission(currentUser, targetUser);

        targetUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(targetUser);
    }

    @Override
    public void restoreUser(Integer id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        // check if account is INACTIVE
        if (existingUser.getStatus() != UserStatus.INACTIVE) {
            throw new RuntimeException("Tài khoản này không ở trạng thái bị xoá!");
        }

        existingUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(existingUser);
    }

    @Override
    public void lockUser(Integer id) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        User currentUser = getCurrentLoggedInUser();

        validatePermission(currentUser, targetUser);

        // check if account is INACTIVE
        if (targetUser.getStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("Không thể khoá tài khoản đã bị xoá!");
        }

        // check if account is LOCKED
        if (targetUser.getStatus() == UserStatus.LOCKED) {
            throw new RuntimeException("Tài khoản này đã bị khoá từ trước!");
        }

        targetUser.setStatus(UserStatus.LOCKED);
        userRepository.save(targetUser);
    }

    @Override
    public void unlockUser(Integer id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        // unlocking is only permitted if the account is LOCKED
        if (existingUser.getStatus() != UserStatus.LOCKED) {
            throw new RuntimeException("Tài khoản này không ở trạng thái bị khoá!");
        }

        existingUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(existingUser);
    }

    private User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("Không thể xác thực người dùng hiện tại!");
        }

        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đang đăng nhập!"));
    }

    private void validatePermission(User currentUser, User targetUser) {
        // rule 1: Protecting Root Admin (ID = 1)
        if (targetUser.getId() == 1) {
            // if the person performing the action is NOT the Root Admin, throw exception
            if (!currentUser.getId().equals(1)) {
                throw new RuntimeException("Lỗi bảo mật: Không ai được phép can thiệp vào tài khoản Admin gốc!");
            }
        }

        // rule 2: Protecting Admin (Admins are not allowed to touch other Admins)
        if (targetUser.getRole() == Role.ADMIN && targetUser.getId() != 1) {
            // if the person performing the action is an Admin, but is trying to edit a different Admin
            if (currentUser.getRole() == Role.ADMIN && !currentUser.getId().equals(targetUser.getId())) {
                throw new RuntimeException("Hành động bị từ chối: Quản trị viên không được phép can thiệp vào tài khoản của Quản trị viên khác!");
            }
            // if Manager touch the Admin
            if (currentUser.getRole() == Role.MANAGER) {
                throw new RuntimeException("Hành động bị từ chối: Quản lý không có quyền thao tác lên Quản trị viên!");
            }
        }

        // rule 3: Protecting Manager (Managers are not allowed to touch other Managers)
        if (targetUser.getRole() == Role.MANAGER) {
            if (currentUser.getRole() == Role.MANAGER && !currentUser.getId().equals(targetUser.getId())) {
                throw new RuntimeException("Hành động bị từ chối: Quản lý không được can thiệp vào tài khoản của Quản lý khác!");
            }
        }
    }

    private void validateRoleAssignment(User currentUser, Role requestedRole) {
        if (requestedRole == null) return;

        // Managers are not allowed to create or grant Admin/Manager
        if (currentUser.getRole() == Role.MANAGER) {
            if (requestedRole == Role.ADMIN || requestedRole == Role.MANAGER) {
                throw new RuntimeException("Hành động bị từ chối: Quản lý chỉ được phép tạo hoặc cấp quyền USER!");
            }
        }

        // a regular admin (ID != 1) is not allowed to create a new Admin
        if (currentUser.getRole() == Role.ADMIN && currentUser.getId() != 1) {
            if (requestedRole == Role.ADMIN) {
                throw new RuntimeException("Hành động bị từ chối: Chỉ Admin gốc mới có quyền tạo thêm Quản trị viên hệ thống!");
            }
        }
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roleName(user.getRole() != null ? user.getRole().name() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .build();
    }
}

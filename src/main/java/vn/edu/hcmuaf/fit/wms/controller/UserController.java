package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.UserProfileUpdateDTO;
import vn.edu.hcmuaf.fit.wms.dto.UserRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.UserResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.Role;
import vn.edu.hcmuaf.fit.wms.entity.enums.UserStatus;
import vn.edu.hcmuaf.fit.wms.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Các API quản lý người dùng")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getALlUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<UserResponseDTO> userPage = userService.getAllUsers(keyword, status, role, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", userPage));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Integer id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo người dùng mới thành công", createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Integer id, @Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO updatedUser = userService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá tài khoản thành công", ""));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> restoreUser(@PathVariable Integer id) {
        userService.restoreUser(id);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục tài khoản thành công", ""));
    }

    @PutMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> lockUser(@PathVariable Integer id) {
        userService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success("Khoá tài khoản thành công", ""));
    }

    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> unlockUser(@PathVariable Integer id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success("Mở khoá tài khoản thành công", ""));
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyProfile() {
        UserResponseDTO profile = userService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateMyProfile(@Valid @RequestBody UserProfileUpdateDTO requestDTO) {
        UserResponseDTO updatedProfile = userService.updateMyProfile(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công", updatedProfile));
    }
}

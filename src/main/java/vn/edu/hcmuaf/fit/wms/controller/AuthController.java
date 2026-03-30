package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.AuthRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.AuthResponseDTO;
import vn.edu.hcmuaf.fit.wms.security.CustomUserDetailsService;
import vn.edu.hcmuaf.fit.wms.security.JwtService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@RequestBody AuthRequestDTO request) {
        // check username, password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // get user details
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getUsername());

        // create token
        String token = jwtService.generateToken(userDetails);

        // prepare data
        AuthResponseDTO data = AuthResponseDTO.builder()
                .username(userDetails.getUsername())
                .token(token)
                .build();

        // create response
        ApiResponse<AuthResponseDTO> response = ApiResponse.success("Đăng nhập thành công", data);

        // return token for client
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Dang xuat thanh cong", ""));
    }
}

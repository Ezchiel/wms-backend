package vn.edu.hcmuaf.fit.wms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.entity.User;
import vn.edu.hcmuaf.fit.wms.entity.UserStatus;
import vn.edu.hcmuaf.fit.wms.repository.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        // check if account is INACTIVE
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new DisabledException("Tài khoản đã bị xoá");
        }

        // check if account is LOCKED
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("Tài khoản đã bị tạm khoá");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}

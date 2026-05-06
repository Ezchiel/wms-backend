package vn.edu.hcmuaf.fit.wms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.entity.enums.Role;
import vn.edu.hcmuaf.fit.wms.entity.User;
import vn.edu.hcmuaf.fit.wms.entity.enums.UserStatus;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageLocationRepository locationRepository;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setEmail("admin@gmail.com");
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);

            userRepository.save(admin);

            System.out.println("[Important] Initialized root admin successfully!");
        }

        // Auto create Receiving Dock
        if (locationRepository.findFirstByLocationType(LocationType.RECEIVING_DOCK).isEmpty()) {
            locationRepository.save(StorageLocation.builder()
                    .barcode("SYS_RCV_DOCK")
                    .zone("DOCK")
                    .rack("IN")
                    .shelf("01")
                    .description("Khu vực nhận hàng mặc định của hệ thống")
                    .locationType(LocationType.RECEIVING_DOCK)
                    .build());
        }

        // Auto create Shipping Dock
        if (locationRepository.findFirstByLocationType(LocationType.SHIPPING_DOCK).isEmpty()) {
            locationRepository.save(StorageLocation.builder()
                    .barcode("SYS_SHIP_DOCK")
                    .zone("DOCK")
                    .rack("OUT")
                    .shelf("01")
                    .description("Khu vực xuất hàng mặc định của hệ thống")
                    .locationType(LocationType.SHIPPING_DOCK)
                    .build());
        }
    }
}

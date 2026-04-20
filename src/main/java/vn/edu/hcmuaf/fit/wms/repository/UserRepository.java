package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.enums.Role;
import vn.edu.hcmuaf.fit.wms.entity.User;
import vn.edu.hcmuaf.fit.wms.entity.enums.UserStatus;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND ((:status IS NOT NULL AND u.status = :status) OR (:status IS NULL AND u.status != 'INACTIVE')) " +
            "AND (:roleId IS NULL OR u.role = :roleId)")
    Page<User> searchAndFilterUsers(@Param("keyword") String keyword,
                                    @Param("status") UserStatus status,
                                    @Param("roleId") Role role,
                                    Pageable pageable);
}

package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryCheck;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;

import java.time.LocalDate;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
    boolean existsByCheckCode(String checkCode);

    @Query("SELECT c FROM InventoryCheck c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:keyword IS NULL OR LOWER(c.checkCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:createdBy IS NULL OR c.createdBy = :createdBy) AND " +
            "(:fromDate IS NULL OR CAST(c.checkDate AS date) >= :fromDate) AND " +
            "(:toDate IS NULL OR CAST(c.checkDate AS date) <= :toDate)")
    Page<InventoryCheck> searchChecks(
            @Param("keyword") String keyword,
            @Param("status") CheckStatus status,
            @Param("createdBy") String createdBy,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );
}

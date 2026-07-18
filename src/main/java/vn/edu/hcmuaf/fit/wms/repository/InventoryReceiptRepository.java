package vn.edu.hcmuaf.fit.wms.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryReceipt;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    boolean existsByReceiptCode(String receiptCode);

    @Query("SELECT r FROM InventoryReceipt r WHERE " +
            "(:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(r.receiptCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:assignedTo IS NULL OR r.assignedTo = :assignedTo) " +
            "AND (:unassigned = false OR r.assignedTo IS NULL) " +
            "AND (:fromDate IS NULL OR CAST(r.createdAt AS date) >= :fromDate) " +
            "AND (:toDate IS NULL OR CAST(r.createdAt AS date) <= :toDate)")
    Page<InventoryReceipt> searchInventoryReceipts(
            @Param("keyword") String keyword,
            @Param("status") ReceiptStatus status,
            @Param("assignedTo") String assignedTo,
            @Param("unassigned") boolean unassigned,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("SELECT r FROM InventoryReceipt r WHERE r.status = 'RECEIVING' AND r.assignedTo IS NULL")
    Page<InventoryReceipt> findAvailableReceipts(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InventoryReceipt r WHERE r.id = :id")
    Optional<InventoryReceipt> findByIdWithLock(@Param("id") Long id);
}


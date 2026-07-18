package vn.edu.hcmuaf.fit.wms.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryIssue;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InventoryIssueRepository extends JpaRepository<InventoryIssue, Long> {

    boolean existsByIssueCode(String issueCode);

    @Query("SELECT i FROM InventoryIssue i LEFT JOIN i.customer c WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(i.issueCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<InventoryIssue> searchIssues(
            @Param("keyword") String keyword,
            @Param("status") IssueStatus status,
            Pageable pageable
    );

    /**
     * Lấy danh sách phiếu APPROVED chưa có nhân viên nào nhận (assignedTo = null)
     */
    @Query("SELECT i FROM InventoryIssue i WHERE i.status = 'APPROVED' AND i.assignedTo IS NULL")
    Page<InventoryIssue> findAvailableIssues(Pageable pageable);

    /**
     * Lấy danh sách phiếu APPROVED chưa nhận, hỗ trợ keyword (mã phiếu/tên KH) + date range
     */
    @Query("SELECT i FROM InventoryIssue i LEFT JOIN i.customer c " +
           "WHERE i.status = 'APPROVED' AND i.assignedTo IS NULL " +
           "AND (:keyword IS NULL OR LOWER(i.issueCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:fromDate IS NULL OR CAST(i.issueDate AS date) >= :fromDate) " +
           "AND (:toDate IS NULL OR CAST(i.issueDate AS date) <= :toDate)")
    Page<InventoryIssue> findAvailableIssuesFiltered(
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    /**
     * Tìm phiếu theo ID với pessimistic write lock để tránh race condition khi claim
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryIssue i WHERE i.id = :id")
    Optional<InventoryIssue> findByIdWithLock(@Param("id") Long id);
}

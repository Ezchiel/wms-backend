package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryIssue;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;

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
}

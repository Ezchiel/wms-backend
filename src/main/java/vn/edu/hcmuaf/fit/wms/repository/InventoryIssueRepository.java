package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryIssue;

@Repository
public interface InventoryIssueRepository extends JpaRepository<InventoryIssue, Long> {
    boolean existsByIssueCode(String issueCode);
}

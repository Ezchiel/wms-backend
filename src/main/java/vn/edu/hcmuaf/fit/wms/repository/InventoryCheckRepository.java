package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryCheck;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
}

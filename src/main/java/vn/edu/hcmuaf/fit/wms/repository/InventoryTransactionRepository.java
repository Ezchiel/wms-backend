package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryTransaction;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<InventoryTransaction> findByProductIdAndLocationIdOrderByCreatedAtDesc(Long productId, Long locationId);
}

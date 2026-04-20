package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.dto.LowStockAlertDTO;
import vn.edu.hcmuaf.fit.wms.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCode(String productCode);
    List<Product> findByProductGroupId(Long groupId);

    @Query("SELECT new vn.edu.hcmuaf.fit.wms.dto.LowStockAlertDTO(p.id, p.productCode, p.productName, p.minStockLevel, CAST(COALESCE(SUM(s.quantity), 0) AS LONG)) " +
            "FROM Product p LEFT JOIN InventoryStock s ON p = s.product " +
            "GROUP BY p.id, p.productCode, p.productName, p.minStockLevel " +
            "HAVING COALESCE(SUM(s.quantity), 0) <= p.minStockLevel")
    List<LowStockAlertDTO> findProductsBelowMinStockLevel();
}

package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryReceiptDetail;

import java.math.BigDecimal;

@Repository
public interface InventoryReceiptDetailRepository extends JpaRepository<InventoryReceiptDetail, Long> {

    /**
     * Tính giá nhập trung bình của tất cả sản phẩm thuộc một nhóm sản phẩm (ProductGroup).
     * Dùng để ước tính giá trị tồn kho trong báo cáo StockByGroup.
     */
    @Query("SELECT AVG(d.unitPrice) FROM InventoryReceiptDetail d " +
           "WHERE d.unitPrice IS NOT NULL " +
           "AND d.product.productGroup.id = :groupId")
    BigDecimal findAvgUnitPriceByGroupId(@Param("groupId") Long groupId);
}

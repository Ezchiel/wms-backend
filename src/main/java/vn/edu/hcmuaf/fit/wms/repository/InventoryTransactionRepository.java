package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryTransaction;
import vn.edu.hcmuaf.fit.wms.entity.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<InventoryTransaction> findByProductIdAndLocationIdOrderByCreatedAtDesc(Long productId, Long locationId);

    /**
     * Tổng quantity theo loại giao dịch, khoảng thời gian, tuỳ chọn lọc theo productId.
     * productId = null → tính tổng toàn bộ sản phẩm.
     */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM InventoryTransaction t " +
           "WHERE t.transactionType = :type " +
           "AND t.createdAt BETWEEN :from AND :to " +
           "AND (:productId IS NULL OR t.product.id = :productId)")
    Long sumQuantityByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("productId") Long productId
    );

    /**
     * Lấy danh sách giao dịch trong khoảng thời gian, tuỳ chọn lọc theo productId.
     * Dùng để tính báo cáo NXT theo từng sản phẩm.
     */
    @Query("SELECT t FROM InventoryTransaction t " +
           "WHERE t.createdAt BETWEEN :from AND :to " +
           "AND (:productId IS NULL OR t.product.id = :productId) " +
           "ORDER BY t.product.id, t.createdAt")
    List<InventoryTransaction> findByDateRangeAndProduct(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("productId") Long productId
    );

    /**
     * Lấy danh sách giao dịch trong khoảng thời gian lọc theo nhóm sản phẩm.
     */
    @Query("SELECT t FROM InventoryTransaction t " +
           "WHERE t.createdAt BETWEEN :from AND :to " +
           "AND t.product.productGroup.id = :groupId " +
           "ORDER BY t.product.id, t.createdAt")
    List<InventoryTransaction> findByDateRangeAndGroup(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("groupId") Long groupId
    );

    /**
     * Stock trend theo ngày — tính net change mỗi ngày.
     * RECEIPT = +quantity, ISSUE = -quantity, ADJUST = +/-quantity (theo giá trị lưu).
     * Native query MySQL: DATE(created_at) group by ngày.
     */
    @Query(value =
           "SELECT DATE(t.created_at) AS period, " +
           "  SUM(CASE t.transaction_type " +
           "       WHEN 'RECEIPT' THEN t.quantity " +
           "       WHEN 'ISSUE'   THEN -t.quantity " +
           "       WHEN 'ADJUST'  THEN t.quantity " +
           "       ELSE 0 END) AS net_change " +
           "FROM inventory_transactions t " +
           "WHERE t.created_at BETWEEN :from AND :to " +
           "AND (:productId IS NULL OR t.product_id = :productId) " +
           "GROUP BY DATE(t.created_at) " +
           "ORDER BY DATE(t.created_at)",
           nativeQuery = true)
    List<Object[]> getStockTrendByDay(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("productId") Long productId
    );

    /**
     * Stock trend theo tuần — group by YEAR + WEEK.
     */
    @Query(value =
           "SELECT CONCAT(YEAR(t.created_at), '-W', LPAD(WEEK(t.created_at, 3), 2, '0')) AS period, " +
           "  SUM(CASE t.transaction_type " +
           "       WHEN 'RECEIPT' THEN t.quantity " +
           "       WHEN 'ISSUE'   THEN -t.quantity " +
           "       WHEN 'ADJUST'  THEN t.quantity " +
           "       ELSE 0 END) AS net_change " +
           "FROM inventory_transactions t " +
           "WHERE t.created_at BETWEEN :from AND :to " +
           "AND (:productId IS NULL OR t.product_id = :productId) " +
           "GROUP BY YEAR(t.created_at), WEEK(t.created_at, 3) " +
           "ORDER BY YEAR(t.created_at), WEEK(t.created_at, 3)",
           nativeQuery = true)
    List<Object[]> getStockTrendByWeek(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("productId") Long productId
    );

    /**
     * Stock trend theo tháng — group by YEAR + MONTH.
     */
    @Query(value =
           "SELECT DATE_FORMAT(t.created_at, '%Y-%m') AS period, " +
           "  SUM(CASE t.transaction_type " +
           "       WHEN 'RECEIPT' THEN t.quantity " +
           "       WHEN 'ISSUE'   THEN -t.quantity " +
           "       WHEN 'ADJUST'  THEN t.quantity " +
           "       ELSE 0 END) AS net_change " +
           "FROM inventory_transactions t " +
           "WHERE t.created_at BETWEEN :from AND :to " +
           "AND (:productId IS NULL OR t.product_id = :productId) " +
           "GROUP BY DATE_FORMAT(t.created_at, '%Y-%m') " +
           "ORDER BY DATE_FORMAT(t.created_at, '%Y-%m')",
           nativeQuery = true)
    List<Object[]> getStockTrendByMonth(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("productId") Long productId
    );
}

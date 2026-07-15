package vn.edu.hcmuaf.fit.wms.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    boolean existsBySerialNumber(String serialNumber);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s WHERE s.location.id = :locationId")
    int getCurrentQuantityByLocation(@Param("locationId") Long locationId);

    /**
     * Lấy danh sách productId đang chiếm vị trí (quantity > 0) với khóa PESSIMISTIC_WRITE.
     * Dùng để tránh race-condition khi 2 giao dịch putaway cùng đọc vị trí trống.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT s.product.id FROM InventoryStock s " +
           "WHERE s.location.id = :locationId AND s.quantity > 0")
    List<Long> findDistinctOccupyingProductIdsWithLock(@Param("locationId") Long locationId);

    /**
     * Lấy danh sách productId đang chiếm vị trí (quantity > 0) không dùng khóa (cho đọc DTO/thông tin).
     */
    @Query("SELECT DISTINCT s.product.id FROM InventoryStock s " +
           "WHERE s.location.id = :locationId AND s.quantity > 0")
    List<Long> findDistinctOccupyingProductIds(@Param("locationId") Long locationId);

    /**
     * Tổng tồn kho đúng sản phẩm tại vị trí — dùng thay cho getCurrentQuantityByLocation
     * trong addStock() để kiểm tra capacity chính xác theo đơn vị của sản phẩm đang xét.
     */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s " +
           "WHERE s.location.id = :locationId AND s.product.id = :productId")
    int getCurrentQuantityByLocationAndProduct(@Param("locationId") Long locationId,
                                               @Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s WHERE s.product.id = :productId AND s.location.id = :locationId")
    List<InventoryStock> findByProduct_IdAndLocation_IdWithLock(@Param("productId") Long productId, @Param("locationId") Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s WHERE s.product.id = :productId AND s.location.id = :locationId AND s.batchNo = :batchNo")
    List<InventoryStock> findByProduct_IdAndLocation_IdAndBatchNoWithLock(@Param("productId") Long productId, @Param("locationId") Long locationId, @Param("batchNo") String batchNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s WHERE s.product.id = :productId AND s.location.id = :locationId AND s.batchNo = :batchNo AND s.serialNumber = :serialNumber")
    List<InventoryStock> findByProduct_IdAndLocation_IdAndBatchNoAndSerialNumberWithLock(
            @Param("productId") Long productId,
            @Param("locationId") Long locationId,
            @Param("batchNo") String batchNo,
            @Param("serialNumber") String serialNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s WHERE s.product.id = :productId AND s.location.id = :locationId AND s.batchNo IS NULL AND s.serialNumber = :serialNumber")
    List<InventoryStock> findByProduct_IdAndLocation_IdAndBatchNoIsNullAndSerialNumberWithLock(
            @Param("productId") Long productId,
            @Param("locationId") Long locationId,
            @Param("serialNumber") String serialNumber
    );
    List<InventoryStock> findByProductId(Long productId);
    List<InventoryStock> findByLocation_Id(Long locationId);
    List<InventoryStock> findByLocation_IdAndProduct_Id(Long locationId, Long productId);

    Optional<InventoryStock> findFirstByProduct_IdAndLocation_IdAndBatchNo(Long productId, Long locationId, String batchNo);
    Optional<InventoryStock> findFirstByProduct_IdAndLocation_IdAndBatchNoIsNull(Long productId, Long locationId);
    List<InventoryStock> findByProduct_IdAndLocation_Id(Long productId, Long locationId);
    Optional<InventoryStock> findByProductAndLocationAndBatchNoAndSerialNumber(
            Product product,
            StorageLocation location,
            String batchNo,
            String serialNumber
    );

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s")
    Long getTotalStockQuantity();

    @Query("SELECT s.product.id, SUM(s.quantity) FROM InventoryStock s GROUP BY s.product.id")
    List<Object[]> countTotalStockByProduct();

    @Query("SELECT s.location.zone, SUM(s.quantity) FROM InventoryStock s GROUP BY s.location.zone")
    List<Object[]> countTotalStockByZone();

    /**
     * Tìm các lô hàng có expiryDate không null và sắp hết hạn trước ngày cutoffDate.
     * Sắp xếp theo ngày hết hạn tăng dần (gần nhất lên đầu).
     */
    @Query("SELECT s FROM InventoryStock s " +
           "WHERE s.expiryDate IS NOT NULL " +
           "AND s.expiryDate <= :cutoffDate " +
           "AND s.quantity > 0 " +
           "ORDER BY s.expiryDate ASC")
    List<InventoryStock> findExpiringSoon(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Tổng hợp tồn kho nhóm theo ProductGroup.
     * Trả về: [groupId, groupCode, groupName, totalQuantity]
     */
    @Query("SELECT p.productGroup.id, p.productGroup.groupCode, p.productGroup.groupName, SUM(s.quantity) " +
           "FROM InventoryStock s " +
           "JOIN s.product p " +
           "GROUP BY p.productGroup.id, p.productGroup.groupCode, p.productGroup.groupName " +
           "ORDER BY p.productGroup.groupCode")
    List<Object[]> getStockByProductGroup();

    /**
     * Tổng tồn kho theo từng sản phẩm, bao gồm productCode, trả về toàn bộ thông tin cần cho NXT.
     * Trả về: [productId, productCode, productName, totalQuantity]
     */
    @Query("SELECT p.id, p.productCode, p.productName, COALESCE(SUM(s.quantity), 0) " +
           "FROM Product p " +
           "LEFT JOIN InventoryStock s ON s.product.id = p.id " +
           "WHERE (:productId IS NULL OR p.id = :productId) " +
           "GROUP BY p.id, p.productCode, p.productName " +
           "ORDER BY p.productCode")
    List<Object[]> getCurrentStockPerProduct(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s " +
           "WHERE s.product.id = :productId " +
           "AND s.location.locationType NOT IN (vn.edu.hcmuaf.fit.wms.entity.enums.LocationType.RECEIVING_DOCK, vn.edu.hcmuaf.fit.wms.entity.enums.LocationType.SHIPPING_DOCK)")
    Integer getTotalAvailableStock(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s JOIN s.location l " +
           "WHERE s.product.id = :productId " +
           "AND s.quantity > 0 " +
           "AND l.locationType NOT IN (vn.edu.hcmuaf.fit.wms.entity.enums.LocationType.RECEIVING_DOCK, vn.edu.hcmuaf.fit.wms.entity.enums.LocationType.SHIPPING_DOCK) " +
           "AND (:batchNo IS NULL OR :batchNo = '' OR s.batchNo = :batchNo)")
    List<InventoryStock> findStockForAllocationWithLock(@Param("productId") Long productId, @Param("batchNo") String batchNo);
}

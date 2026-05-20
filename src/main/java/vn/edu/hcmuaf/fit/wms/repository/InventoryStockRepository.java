package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    List<InventoryStock> findByProductId(Long productId);
    Optional<InventoryStock> findFirstByProduct_IdAndLocation_IdAndBatchNo(Long productId, Long locationId, String batchNo);
    Optional<InventoryStock> findFirstByProduct_IdAndLocation_IdAndBatchNoIsNull(Long productId, Long locationId);
    List<InventoryStock> findByProduct_IdAndLocation_Id(Long productId, Long locationId);
    Optional<InventoryStock> findByProductAndLocation(Product product, StorageLocation location);
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
     * The total inventory of a product at a specific location (regardless of batch).
     */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s " +
            "WHERE s.product.id = :productId AND s.location.id = :locationId")
    Integer sumQuantityByProductAndLocation(
            @Param("productId") Long productId,
            @Param("locationId") Long locationId
    );
}

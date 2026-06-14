package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
    Optional<StorageLocation> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
    Optional<StorageLocation> findFirstByLocationType(LocationType locationType);

    @Query(value = "SELECT sl.* FROM storage_locations sl\n" +
                   "LEFT JOIN inventory_stocks st ON sl.id = st.location_id AND st.product_id = :productId\n" +
                   "WHERE sl.is_full = false AND sl.location_type = 'STORAGE'\n" +
                   "ORDER BY\n" +
                   "    (st.product_id IS NOT NULL) DESC,\n" +
                   "    sl.path_sequence ASC\n" +
                   "LIMIT 1\n", nativeQuery = true)
    Optional<StorageLocation> findOptimalLocationForPutaway(@Param("productId") Long productId);

    List<StorageLocation> findByBarcodeIn(List<String> barcodes);

    @Query("SELECT sl FROM StorageLocation sl WHERE " +
            "(:keyword IS NULL OR LOWER(sl.zone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(sl.rack) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(sl.shelf) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(sl.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:isAvailableOnly = false OR sl.isFull = false) " +
            "AND (:locationType IS NULL OR sl.locationType = :locationType)")
    Page<StorageLocation> searchLocations(@Param("keyword") String keyword,
                                          @Param("isAvailableOnly") boolean isAvailableOnly,
                                          @Param("locationType") LocationType locationType,
                                          Pageable pageable);

    /**
     * Thống kê tỷ lệ sử dụng vị trí kho nhóm theo zone.
     * Trả về: [zone, totalLocations, fullLocations, emptyLocations]
     */
    @Query("SELECT l.zone, " +
           "COUNT(l), " +
           "SUM(CASE WHEN l.isFull = true THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN l.isFull = false THEN 1L ELSE 0L END) " +
           "FROM StorageLocation l " +
           "WHERE l.locationType = 'STORAGE' " +
           "GROUP BY l.zone " +
           "ORDER BY l.zone")
    List<Object[]> getUtilizationByZone();
}

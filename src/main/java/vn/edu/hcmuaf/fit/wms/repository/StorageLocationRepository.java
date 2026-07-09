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

    @Query(value = """
        SELECT sl.* FROM storage_locations sl
        LEFT JOIN (
            SELECT location_id, SUM(quantity) AS total_qty
            FROM inventory_stocks GROUP BY location_id
        ) st ON sl.id = st.location_id
        LEFT JOIN (
            SELECT location_id, product_id
            FROM inventory_stocks
            WHERE quantity > 0
            GROUP BY location_id, product_id
            HAVING SUM(quantity) > 0
        ) occ ON occ.location_id = sl.id
        WHERE sl.location_type = 'STORAGE'
          AND (sl.max_capacity IS NULL OR COALESCE(st.total_qty, 0) < sl.max_capacity)
          AND (occ.location_id IS NULL OR occ.product_id = :productId)
        ORDER BY
            (occ.product_id = :productId) DESC,
            sl.path_sequence ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<StorageLocation> findOptimalLocationForPutaway(@Param("productId") Long productId);

    List<StorageLocation> findByBarcodeIn(List<String> barcodes);

    @Query("SELECT sl FROM StorageLocation sl WHERE " +
            "(:keyword IS NULL OR LOWER(sl.zone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(sl.rack) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(sl.shelf) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                  OR LOWER(sl.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:isAvailableOnly = false OR sl.maxCapacity IS NULL OR " +
            "    (SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s WHERE s.location.id = sl.id) < sl.maxCapacity) " +
            "AND (:locationType IS NULL OR sl.locationType = :locationType)")
    Page<StorageLocation> searchLocations(@Param("keyword") String keyword,
                                          @Param("isAvailableOnly") boolean isAvailableOnly,
                                          @Param("locationType") LocationType locationType,
                                          Pageable pageable);

    /**
     * Thống kê tỷ lệ sử dụng vị trí kho nhóm theo zone.
     * Trả về: [zone, totalLocations, totalQuantity, totalCapacity, fullLocations, emptyLocations]
     */
    @Query(value = """
        SELECT sl.zone,
               COUNT(sl.id)                                   AS total_locations,
               COALESCE(SUM(st.total_qty), 0)                 AS total_quantity,
               COALESCE(SUM(sl.max_capacity), 0)               AS total_capacity,
               SUM(CASE WHEN sl.max_capacity IS NOT NULL
                         AND COALESCE(st.total_qty,0) >= sl.max_capacity
                        THEN 1 ELSE 0 END)                     AS full_locations,
               SUM(CASE WHEN COALESCE(st.total_qty, 0) = 0 THEN 1 ELSE 0 END) AS empty_locations
        FROM storage_locations sl
        LEFT JOIN (
            SELECT location_id, SUM(quantity) AS total_qty
            FROM inventory_stocks GROUP BY location_id
        ) st ON sl.id = st.location_id
        WHERE sl.location_type = 'STORAGE'
        GROUP BY sl.zone
        ORDER BY sl.zone
        """, nativeQuery = true)
    List<Object[]> getUtilizationByZone();

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s WHERE s.location.id = :locationId")
    int getCurrentQuantity(@Param("locationId") Long locationId);
}

package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
    Optional<StorageLocation> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
    Page<StorageLocation> findByIsFullFalse(Pageable pageable);

    @Query(value = "SELECT sl.* FROM storage_locations sl\n" +
                   "LEFT JOIN inventory_stocks st ON sl.id = st.location_id AND st.product_id = :productId\n" +
                   "WHERE sl.is_full = false\n" +
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
            "AND (:isAvailableOnly = false OR sl.isFull = false)")
    Page<StorageLocation> searchLocations(@Param("keyword") String keyword,
                                          @Param("isAvailableOnly") boolean isAvailableOnly,
                                          Pageable pageable);
}

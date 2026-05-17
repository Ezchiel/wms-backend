package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.InventoryReceipt;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;

@Repository
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    boolean existsByReceiptCode(String receiptCode);

    @Query("SELECT r FROM InventoryReceipt r WHERE " +
            "(:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(r.receiptCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
    Page<InventoryReceipt> searchInventoryReceipts(@Param("keyword") String keyword, @Param("status")ReceiptStatus status, Pageable pageable);
}

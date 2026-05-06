package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.Lpn;

import java.util.Optional;

@Repository
public interface LpnRepository extends JpaRepository<Lpn, Long> {
    Optional<Lpn> findByLpnCode(String lpnCode);
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    Optional<Lpn> findByReceipt_Id(Long receiptId);
}
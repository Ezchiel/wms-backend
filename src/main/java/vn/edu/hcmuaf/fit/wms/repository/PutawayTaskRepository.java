package vn.edu.hcmuaf.fit.wms.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.PutawayTask;

import java.util.List;
import java.util.Optional;

@Repository
public interface PutawayTaskRepository extends JpaRepository<PutawayTask, Long> {

    List<PutawayTask> findByReceipt_Id(Long receiptId);

    @Query("SELECT t FROM PutawayTask t WHERE t.status = 'PENDING' AND t.assignedTo IS NULL")
    Page<PutawayTask> findAvailableTasks(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PutawayTask t WHERE t.id = :id")
    Optional<PutawayTask> findByIdWithLock(@Param("id") Long id);
}

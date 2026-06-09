package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.PickingTask;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;

import java.util.List;

@Repository
public interface PickingTaskRepository extends JpaRepository<PickingTask, Long> {

    List<PickingTask> findByInventoryIssue_Id(Long issueId);

    List<PickingTask> findByAssignedToAndStatus(String username, PickingTaskStatus status);

    @Query("SELECT t FROM PickingTask t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:assignedTo IS NULL OR t.assignedTo = :assignedTo)")
    Page<PickingTask> searchTasks(
            @Param("status") PickingTaskStatus status,
            @Param("assignedTo") String assignedTo,
            Pageable pageable
    );
}

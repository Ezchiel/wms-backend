package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.PickingConfirmRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PickingTaskResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;

import java.util.List;

public interface PickingService {

    List<PickingTaskResponseDTO> assignPickingTasks(Long issueId);

    Page<PickingTaskResponseDTO> getMyPickingTasks(String username, PickingTaskStatus status, int page, int size);

    Page<PickingTaskResponseDTO> searchTasks(PickingTaskStatus status, String assignedTo, int page, int size);

    PickingTaskResponseDTO getPickingTaskById(Long taskId);

    PickingTaskResponseDTO confirmPickingTask(PickingConfirmRequestDTO request, String username);

    void checkAndCompleteIssue(Long issueId);
}

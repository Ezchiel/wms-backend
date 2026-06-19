package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.PutawayTaskConfirmRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawayTaskResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.PutawayTaskStatus;

import java.util.List;

public interface PutawayTaskService {

    Page<PutawayTaskResponseDTO> getAllTasks(PutawayTaskStatus status, String assignedTo, int page, int size);

    Page<PutawayTaskResponseDTO> getAvailableTasks(int page, int size);

    PutawayTaskResponseDTO getTaskById(Long taskId);

    PutawayTaskResponseDTO claimTask(Long taskId, String username);

    PutawayTaskResponseDTO confirmTask(PutawayTaskConfirmRequestDTO request, String username);

    void checkAndCompleteReceipt(Long receiptId);
}

package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.PickingConfirmRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PickingTaskResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.Role;
import vn.edu.hcmuaf.fit.wms.repository.InventoryIssueRepository;
import vn.edu.hcmuaf.fit.wms.repository.PickingTaskRepository;
import vn.edu.hcmuaf.fit.wms.repository.UserRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryIssueService;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;
import vn.edu.hcmuaf.fit.wms.service.PickingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PickingServiceImpl implements PickingService {

    private final PickingTaskRepository pickingTaskRepository;
    private final InventoryIssueRepository issueRepository;
    private final UserRepository userRepository;
    private final InventoryIssueService issueService;
    private final InventoryStockService stockService;

    @Override
    @Transactional
    public List<PickingTaskResponseDTO> assignPickingTasks(Long issueId) {
        InventoryIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất kho với id: " + issueId));

        if (issue.getStatus() != IssueStatus.APPROVED) {
            throw new RuntimeException("Chỉ có thể phân công lấy hàng với phiếu đang ở trạng thái APPROVED!");
        }

        // Tạo picking tasks
        List<PickingTask> tasks = issue.getDetails().stream().map(detail -> {
            return PickingTask.builder()
                    .inventoryIssue(issue)
                    .issueDetail(detail)
                    .product(detail.getProduct())
                    .location(detail.getLocation())
                    .requiredQuantity(detail.getQuantity())
                    .pickedQuantity(0)
                    .status(PickingTaskStatus.PENDING)
                    .build();
        }).collect(Collectors.toList());

        pickingTaskRepository.saveAll(tasks);

        // Chuyển trạng thái phiếu xuất kho sang PICKING
        issue.setStatus(IssueStatus.PICKING);
        issueRepository.save(issue);

        return tasks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public Page<PickingTaskResponseDTO> getMyPickingTasks(String username, PickingTaskStatus status, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + username));

        // Nếu là Manager hoặc Admin, họ có thể lấy toàn bộ task (assignedTo = null để lấy tất cả)
        String assignedTo = (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) ? null : username;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return pickingTaskRepository.searchTasks(status, assignedTo, pageable).map(this::mapToDTO);
    }

    @Override
    public Page<PickingTaskResponseDTO> searchTasks(PickingTaskStatus status, String assignedTo, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        String targetAssignedTo = (assignedTo == null || assignedTo.isBlank()) ? null : assignedTo;
        return pickingTaskRepository.searchTasks(status, targetAssignedTo, pageable).map(this::mapToDTO);
    }

    @Override
    public PickingTaskResponseDTO getPickingTaskById(Long taskId) {
        PickingTask task = pickingTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy picking task với id: " + taskId));
        return mapToDTO(task);
    }

    @Override
    @Transactional
    public PickingTaskResponseDTO confirmPickingTask(PickingConfirmRequestDTO request, String username) {
        if (request.getPickedQuantity() == null || request.getPickedQuantity() < 0) {
            throw new RuntimeException("Số lượng thực tế đã lấy phải lớn hơn hoặc bằng 0!");
        }

        PickingTask task = pickingTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy picking task với id: " + request.getTaskId()));

        InventoryIssue issue = task.getInventoryIssue();
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            throw new RuntimeException("Không thể xác nhận task của phiếu xuất kho đã bị HỦY!");
        }
        if (issue.getStatus() == IssueStatus.COMPLETED) {
            throw new RuntimeException("Không thể xác nhận task của phiếu xuất kho đã HOÀN THÀNH!");
        }

        // Ưu tiên kiểm tra tồn kho hệ thống trước khi xác nhận lấy hàng
        if (request.getPickedQuantity() > 0) {
            Integer availableStock = stockService.getCurrentStockQuantity(task.getProduct().getId(), task.getLocation().getId());
            if (availableStock == null || availableStock < request.getPickedQuantity()) {
                throw new RuntimeException("Không đủ tồn kho hệ thống tại vị trí để lấy hàng! Hiện có: " 
                        + (availableStock != null ? availableStock : 0) + ", yêu cầu: " + request.getPickedQuantity());
            }
        }

        // Cập nhật thông tin task
        task.setPickedQuantity(request.getPickedQuantity());
        task.setPickedAt(LocalDateTime.now());
        task.setNote(request.getNote());

        if (task.getAssignedTo() == null || task.getAssignedTo().isBlank()) {
            task.setAssignedTo(username);
        }

        if (request.getPickedQuantity() >= task.getRequiredQuantity()) {
            task.setStatus(PickingTaskStatus.DONE);
        } else {
            task.setStatus(PickingTaskStatus.FAILED);
        }

        pickingTaskRepository.save(task);

        // Kiểm tra và tự động hoàn thành phiếu xuất kho
        checkAndCompleteIssue(issue.getId());

        return mapToDTO(task);
    }

    @Override
    @Transactional
    public void checkAndCompleteIssue(Long issueId) {
        List<PickingTask> tasks = pickingTaskRepository.findByInventoryIssue_Id(issueId);
        if (tasks.isEmpty()) return;

        boolean allDone = tasks.stream().allMatch(t -> t.getStatus() == PickingTaskStatus.DONE);

        if (allDone) {
            issueService.confirmIssue(issueId);
        }
    }

    private PickingTaskResponseDTO mapToDTO(PickingTask task) {
        if (task == null) return null;
        return PickingTaskResponseDTO.builder()
                .id(task.getId())
                .issueId(task.getInventoryIssue().getId())
                .issueCode(task.getInventoryIssue().getIssueCode())
                .productId(task.getProduct().getId())
                .productName(task.getProduct().getProductName())
                .productCode(task.getProduct().getProductCode())
                .locationId(task.getLocation().getId())
                .locationBarcode(task.getLocation().getBarcode())
                .locationDescription(task.getLocation().getDescription())
                .requiredQuantity(task.getRequiredQuantity())
                .pickedQuantity(task.getPickedQuantity())
                .status(task.getStatus())
                .assignedTo(task.getAssignedTo())
                .note(task.getNote())
                .pickedAt(task.getPickedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }
}

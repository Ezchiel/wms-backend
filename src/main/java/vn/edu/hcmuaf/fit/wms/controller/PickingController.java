package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.PickingConfirmRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PickingTaskResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;
import vn.edu.hcmuaf.fit.wms.service.PickingService;

import java.util.List;

@RestController
@RequestMapping("/api/picking")
@RequiredArgsConstructor
public class PickingController {

    private final PickingService pickingService;

    /**
     * GET /api/picking/tasks
     * Lấy danh sách task (filter theo status, assignedTo)
     */
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<PickingTaskResponseDTO>>> getAllTasks(
            @RequestParam(required = false) PickingTaskStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PickingTaskResponseDTO> result = pickingService.searchTasks(status, assignedTo, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách picking task thành công", result));
    }

    /**
     * GET /api/picking/tasks/my
     * Nhân viên lấy task được giao cho mình (hoặc tất cả nếu là manager/admin)
     */
    @GetMapping("/tasks/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<PickingTaskResponseDTO>>> getMyTasks(
            @RequestParam(required = false) PickingTaskStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        String username = authentication.getName();
        Page<PickingTaskResponseDTO> result = pickingService.getMyPickingTasks(username, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách task được giao thành công", result));
    }

    /**
     * GET /api/picking/tasks/{taskId}
     * Xem chi tiết một picking task
     */
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PickingTaskResponseDTO>> getTaskById(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết picking task thành công",
                pickingService.getPickingTaskById(taskId)
        ));
    }

    /**
     * PUT /api/picking/tasks/{taskId}/confirm
     * Nhân viên xác nhận đã lấy hàng xong
     */
    @PutMapping("/tasks/{taskId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PickingTaskResponseDTO>> confirmTask(
            @PathVariable Long taskId,
            @RequestBody PickingConfirmRequestDTO request,
            Authentication authentication
    ) {
        request.setTaskId(taskId);
        String username = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận hoàn thành picking task thành công",
                pickingService.confirmPickingTask(request, username)
        ));
    }
}

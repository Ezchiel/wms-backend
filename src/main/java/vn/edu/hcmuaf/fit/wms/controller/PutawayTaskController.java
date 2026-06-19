package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.PutawayTaskConfirmRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawayTaskResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.PutawayTaskStatus;
import vn.edu.hcmuaf.fit.wms.service.PutawayTaskService;

import java.util.Objects;

@RestController
@RequestMapping("/api/putaway/tasks")
@RequiredArgsConstructor
@Tag(name = "Putaway Tasks", description = "Các API quản lý nhiệm vụ cất hàng nhập kho")
public class PutawayTaskController {

        private final PutawayTaskService putawayTaskService;

        /**
         * Lấy danh sách tất cả các task (filter tùy chọn theo status và assignedTo).
         */
        @GetMapping
        public ResponseEntity<ApiResponse<?>> getAllTasks(
                        @RequestParam(required = false) PutawayTaskStatus status,
                        @RequestParam(required = false) String assignedTo,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Lấy danh sách task cất hàng thành công",
                                putawayTaskService.getAllTasks(status, assignedTo, page, size)));
        }

        /**
         * Lấy danh sách các task PENDING chưa có người nhận.
         */
        @GetMapping("/available")
        public ResponseEntity<ApiResponse<?>> getAvailableTasks(
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Lấy danh sách task chờ cất hàng thành công",
                                putawayTaskService.getAvailableTasks(page, size)));
        }

        /**
         * Xem chi tiết một task cất hàng.
         */
        @GetMapping("/{taskId}")
        public ResponseEntity<ApiResponse<PutawayTaskResponseDTO>> getTaskById(
                        @PathVariable Long taskId) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Lấy chi tiết task thành công",
                                putawayTaskService.getTaskById(taskId)));
        }

        /**
         * Nhân viên nhận task cất hàng (pessimistic lock chống race condition).
         */
        @PutMapping("/{taskId}/claim")
        public ResponseEntity<ApiResponse<PutawayTaskResponseDTO>> claimTask(
                        @PathVariable Long taskId) {
                String username = Objects.requireNonNull(
                                SecurityContextHolder.getContext().getAuthentication()).getName();
                return ResponseEntity.ok(ApiResponse.success(
                                "Nhận task cất hàng thành công",
                                putawayTaskService.claimTask(taskId, username)));
        }

        /**
         * Xác nhận cất hàng lên kệ.
         */
        @PutMapping("/{taskId}/confirm")
        public ResponseEntity<ApiResponse<PutawayTaskResponseDTO>> confirmTask(
                        @PathVariable Long taskId,
                        @RequestBody PutawayTaskConfirmRequestDTO request) {
                String username = Objects.requireNonNull(
                                SecurityContextHolder.getContext().getAuthentication()).getName();

                request.setTaskId(taskId);
                return ResponseEntity.ok(ApiResponse.success(
                                "Cất hàng thành công",
                                putawayTaskService.confirmTask(request, username)));
        }
}

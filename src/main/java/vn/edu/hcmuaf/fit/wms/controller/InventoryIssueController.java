package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;
import vn.edu.hcmuaf.fit.wms.service.InventoryIssueService;
import vn.edu.hcmuaf.fit.wms.service.PickingService;
import vn.edu.hcmuaf.fit.wms.dto.PickingTaskResponseDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class InventoryIssueController {

    private final InventoryIssueService issueService;
    private final PickingService pickingService;

    /**
     * GET /api/issues
     * Lấy danh sách phiếu xuất kho có phân trang và filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<IssueResponseDTO>>> getAllIssues(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "issueDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<IssueResponseDTO> result = issueService.getAllIssues(keyword, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phiếu xuất kho thành công", result));
    }

    /**
     * GET /api/issues/{id}
     * Lấy chi tiết một phiếu xuất kho
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin phiếu xuất kho thành công",
                issueService.getIssueById(id)
        ));
    }

    /**
     * POST /api/issues
     * Tạo phiếu xuất kho mới (trạng thái DRAFT)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<IssueResponseDTO>> createIssue(@RequestBody IssueRequestDTO requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo phiếu xuất kho thành công",
                issueService.createIssue(requestDTO)
        ));
    }

    /**
     * PUT /api/issues/{id}/approve
     * Quản lý duyệt phiếu: DRAFT → APPROVED
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> approveIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Duyệt phiếu xuất kho thành công",
                issueService.approveIssue(id)
        ));
    }

    /**
     * PUT /api/issues/{id}/confirm
     * Xác nhận xuất hàng, trừ tồn kho: APPROVED → COMPLETED
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> confirmIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận xuất hàng thành công",
                issueService.confirmIssue(id)
        ));
    }

    /**
     * PUT /api/issues/{id}/cancel
     * Huỷ phiếu: DRAFT/APPROVED → CANCELLED
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> cancelIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Huỷ phiếu xuất kho thành công",
                issueService.cancelIssue(id)
        ));
    }

    /**
     * PUT /api/issues/{id}/assign-picking
     * Quản lý phân công lấy hàng: APPROVED → PICKING
     */
    @PutMapping("/{id}/assign-picking")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<PickingTaskResponseDTO>>> assignPicking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Phân công lấy hàng thành công",
                pickingService.assignPickingTasks(id)
        ));
    }
}

package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;
import vn.edu.hcmuaf.fit.wms.service.InventoryIssueService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Inventory Issues", description = "Các API quản lý phiếu xuất kho")
public class InventoryIssueController {

    private final InventoryIssueService issueService;

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
     * Quản lý duyệt phiếu: DRAFT → PICKING (tự động tạo PickingTask)
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> approveIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Duyệt phiếu và tạo lệnh lấy hàng thành công",
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
     * GET /api/issues/available
     * Nhân viên xem danh sách phiếu đang chờ được nhận (status = APPROVED, assignedTo = null)
     * Hỗ trợ thêm: keyword, sortBy, sortDir, fromDate, toDate
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<java.util.List<IssueResponseDTO>>> getAvailableIssues(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "issueDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        Page<IssueResponseDTO> result = issueService.getAvailableIssuesFiltered(
                keyword, page, size, sortBy, sortDir, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phiếu chờ nhận thành công", result));
    }

    /**
     * PUT /api/issues/{id}/claim
     * Nhân viên nhận phiếu để bắt đầu picking (APPROVED → PICKING)
     */
    @PutMapping("/{id}/claim")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> claimIssue(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(
                "Nhận phiếu xuất kho thành công",
                issueService.claimIssue(id, username)
        ));
    }

    /**
     * PUT /api/issues/{id}/cancel
     * Huỷ phiếu: DRAFT/APPROVED/PICKING → CANCELLED
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> cancelIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Huỷ phiếu xuất kho thành công",
                issueService.cancelIssue(id)
        ));
    }
}

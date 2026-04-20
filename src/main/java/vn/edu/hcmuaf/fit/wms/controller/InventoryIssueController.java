package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.InventoryIssue;
import vn.edu.hcmuaf.fit.wms.service.InventoryIssueService;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class InventoryIssueController {

    private final InventoryIssueService issueService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IssueResponseDTO>>> getAllIssues() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách phiếu xuất kho thành công",
                issueService.getAllIssues()
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IssueResponseDTO>> createIssue(@RequestBody IssueRequestDTO requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo phiếu xuất kho thành công",
                issueService.createIssue(requestDTO)
        ));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<IssueResponseDTO>> confirmIssue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận phiếu xuất kho thành công",
                issueService.confirmIssue(id)
        ));
    }
}

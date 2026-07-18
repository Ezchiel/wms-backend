package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.CheckRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CheckResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;
import vn.edu.hcmuaf.fit.wms.service.InventoryCheckService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
@Tag(name = "Inventory Checks", description = "Các API quản lý phiếu kiểm kê")
public class InventoryCheckController {

    private final InventoryCheckService checkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CheckResponseDTO>>> getAllChecks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CheckStatus status,
            @RequestParam(required = false) Boolean createdByMe,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách phiếu kiểm kê thành công",
                checkService.getAllChecks(keyword, status, createdByMe, fromDate, toDate, page, size, sortBy, sortDir)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CheckResponseDTO>> createCheck(@RequestBody CheckRequestDTO requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo phiếu kiểm kê thành công",
                checkService.createCheck(requestDTO)
        ));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<CheckResponseDTO>> confirmCheck(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận phiếu kiểm kê thành công",
                checkService.confirmCheck(id)
        ));
    }
}

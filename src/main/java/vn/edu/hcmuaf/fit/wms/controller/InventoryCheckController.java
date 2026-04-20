package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.CheckRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CheckResponseDTO;
import vn.edu.hcmuaf.fit.wms.service.InventoryCheckService;

import java.util.List;

@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
public class InventoryCheckController {

    private final InventoryCheckService checkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CheckResponseDTO>>> getAllChecks() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách phiếu kiểm kê thành công",
                checkService.getAllChecks()
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

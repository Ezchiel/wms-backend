package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockResponseDTO;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-stocks")
@RequiredArgsConstructor
public class InventoryStockController {

    private final InventoryStockService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryStockResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách tồn kho thành công",
                service.getAllStocks()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryStockResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin tồn kho thành công",
                service.getStockById(id)
        ));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryStockResponseDTO>>> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách tồn kho theo sản phẩm thành công",
                service.getStocksByProductId(productId)
        ));
    }
}
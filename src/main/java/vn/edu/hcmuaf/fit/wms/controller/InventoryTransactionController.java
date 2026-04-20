package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.entity.InventoryTransaction;
import vn.edu.hcmuaf.fit.wms.repository.InventoryTransactionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/stock-cards")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionRepository transactionRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getStockCardByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách lịch sử thẻ kho của sản phẩm thành công",
                transactionRepository.findByProductIdOrderByCreatedAtDesc(productId)
        ));
    }

    @GetMapping("/product/{productId}/location/{locationId}")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getStockCardByProductAndLocation(
            @PathVariable Long productId,
            @PathVariable Long locationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách lịch sử thẻ kho của sản phẩm tại vị trí " + locationId + " thành công",
                transactionRepository.findByProductIdAndLocationIdOrderByCreatedAtDesc(productId, locationId)
        ));
    }
}

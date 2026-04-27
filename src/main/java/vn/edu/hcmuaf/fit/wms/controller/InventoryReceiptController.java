package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelResponseDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.InventoryReceipt;
import vn.edu.hcmuaf.fit.wms.service.InventoryReceiptService;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class InventoryReceiptController {

    private final InventoryReceiptService receiptService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReceiptResponseDTO>> createReceipt(@RequestBody ReceiptRequestDTO requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo phiếu nhập kho thành công",
                receiptService.createReceipt(requestDTO)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReceiptResponseDTO>>> getAllReceipts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách phiếu nhập kho thành công",
                receiptService.getAllReceipts()
        ));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<ReceiptResponseDTO>> confirmReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận phiếu nhập kho thành công",
                receiptService.confirmReceipt(id)
        ));
    }

    @PostMapping("/{receiptId}/details/{detailId}/count-and-label")
    public ResponseEntity<ApiResponse<CountAndLabelResponseDTO>> countAndLabel(
            @PathVariable Long receiptId,
            @PathVariable Long detailId,
            @RequestBody CountAndLabelRequestDTO requestDTO) {

        return ResponseEntity.ok(ApiResponse.success(
                "Đã kiểm đếm và tạo lệnh in tem thành công",
                receiptService.countAndLabel(receiptId, detailId, requestDTO)
        ));
    }
}

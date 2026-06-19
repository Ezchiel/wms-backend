package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelResponseDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.service.InventoryReceiptService;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Inventory Receipts", description = "Các API quản lý phiếu nhập kho")
public class InventoryReceiptController {

        private final InventoryReceiptService receiptService;

        @PostMapping
        public ResponseEntity<ApiResponse<ReceiptResponseDTO>> createReceipt(
                        @RequestBody ReceiptRequestDTO requestDTO) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Tạo phiếu nhập kho thành công",
                                receiptService.createReceipt(requestDTO)));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<ReceiptResponseDTO>>> getAllReceipts(
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) ReceiptStatus status,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Lấy danh sách phiếu nhập kho thành công",
                                receiptService.getAllReceipts(keyword, status, page, size, sortBy, sortDir)));
        }

        @PutMapping("/{id}/confirm")
        public ResponseEntity<ApiResponse<ReceiptResponseDTO>> confirmReceipt(@PathVariable Long id) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Xác nhận phiếu nhập kho thành công",
                                receiptService.confirmReceipt(id)));
        }

        @PostMapping("/{receiptId}/details/{detailId}/count-and-label")
        public ResponseEntity<ApiResponse<CountAndLabelResponseDTO>> countAndLabel(
                        @PathVariable Long receiptId,
                        @PathVariable Long detailId,
                        @RequestBody CountAndLabelRequestDTO requestDTO) {

                return ResponseEntity.ok(ApiResponse.success(
                                "Đã kiểm đếm và tạo lệnh in tem thành công",
                                receiptService.countAndLabel(receiptId, detailId, requestDTO)));
        }
}

package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelResponseDTO;
import vn.edu.hcmuaf.fit.wms.dto.OcrReceiptResultDTO;
import vn.edu.hcmuaf.fit.wms.dto.OcrScanRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.service.InventoryReceiptService;
import vn.edu.hcmuaf.fit.wms.service.OcrReceiptService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Inventory Receipts", description = "Các API quản lý phiếu nhập kho")
public class InventoryReceiptController {

        private final InventoryReceiptService receiptService;
        private final OcrReceiptService ocrReceiptService;

        @Operation(summary = "OCR phiếu nhập kho bằng Gemini Vision",
                   description = "Nhận ảnh base64, trích xuất thông tin phiếu nhập kho bằng AI. Không ghi DB.")
        @PostMapping("/ocr-scan")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
        public ResponseEntity<ApiResponse<OcrReceiptResultDTO>> ocrScan(
                        @RequestBody OcrScanRequestDTO requestDTO) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Phân tích ảnh phiếu nhập kho thành công",
                                ocrReceiptService.scanReceipt(requestDTO)));
        }

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
                        @RequestParam(required = false) String assignedFilter,
                        @RequestParam(required = false) LocalDate fromDate,
                        @RequestParam(required = false) LocalDate toDate,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Lấy danh sách phiếu nhập kho thành công",
                                receiptService.getAllReceipts(keyword, status, assignedFilter, fromDate, toDate, page, size, sortBy, sortDir)));
        }

        @GetMapping("/available")
        public ResponseEntity<ApiResponse<List<ReceiptResponseDTO>>> getAvailableReceipts(
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Lấy danh sách phiếu nhập chờ nhận thành công",
                                receiptService.getAvailableReceipts(page, size)));
        }

        @PutMapping("/{id}/confirm")
        public ResponseEntity<ApiResponse<ReceiptResponseDTO>> confirmReceipt(@PathVariable Long id) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Xác nhận phiếu nhập kho thành công",
                                receiptService.confirmReceipt(id)));
        }

        @PutMapping("/{id}/claim")
        public ResponseEntity<ApiResponse<ReceiptResponseDTO>> claimReceipt(@PathVariable Long id) {
                String username = Objects.requireNonNull(
                        SecurityContextHolder.getContext().getAuthentication()).getName();
                return ResponseEntity.ok(ApiResponse.success(
                                "Nhận phiếu kiểm đếm thành công",
                                receiptService.claimReceipt(id, username)));
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

        @PostMapping("/draft")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
        public ResponseEntity<ApiResponse<ReceiptResponseDTO>> createDraftReceipt(
                        @RequestBody ReceiptRequestDTO requestDTO) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Tạo phiếu nháp thành công",
                                receiptService.createDraftReceipt(requestDTO)));
        }

        @PutMapping("/draft/{id}/approve")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
        public ResponseEntity<ApiResponse<ReceiptResponseDTO>> approveDraftReceipt(
                        @PathVariable Long id,
                        @RequestBody ReceiptRequestDTO requestDTO) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Duyệt phiếu nháp thành công",
                                receiptService.approveDraftReceipt(id, requestDTO)));
        }
}


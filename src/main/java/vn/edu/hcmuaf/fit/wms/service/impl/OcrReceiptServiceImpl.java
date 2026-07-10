package vn.edu.hcmuaf.fit.wms.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.OcrReceiptItemDTO;
import vn.edu.hcmuaf.fit.wms.dto.OcrReceiptResultDTO;
import vn.edu.hcmuaf.fit.wms.dto.OcrScanRequestDTO;
import vn.edu.hcmuaf.fit.wms.service.GeminiVisionService;
import vn.edu.hcmuaf.fit.wms.service.OcrReceiptMatchingService;
import vn.edu.hcmuaf.fit.wms.service.OcrReceiptService;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai luồng OCR phiếu nhập kho:
 * 1. Validate kích thước ảnh
 * 2. Gọi GeminiVisionService → raw JSON string
 * 3. Parse JSON → intermediate POJO
 * 4. Match từng item với Product trong hệ thống
 * 5. Match supplier với Partner trong hệ thống
 * 6. Build và trả OcrReceiptResultDTO
 *
 * Mọi lỗi đều được bắt và trả về DTO với warningMessage thay vì throw 500.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrReceiptServiceImpl implements OcrReceiptService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB

    private final GeminiVisionService geminiVisionService;
    private final OcrReceiptMatchingService matchingService;
    private final ObjectMapper objectMapper;

    @Override
    public OcrReceiptResultDTO scanReceipt(OcrScanRequestDTO request) {
        // ----------------------------------------------------------------
        // 1. Validate đầu vào
        // ----------------------------------------------------------------
        if (request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            return buildErrorResult("Dữ liệu ảnh không được để trống.");
        }

        String mimeType = request.getMimeType() != null ? request.getMimeType() : "image/jpeg";

        // Kiểm tra kích thước ảnh (base64 → bytes)
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());
            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                return buildErrorResult("Kích thước ảnh vượt quá 5MB. Vui lòng nén ảnh trước khi gửi.");
            }
        } catch (IllegalArgumentException e) {
            return buildErrorResult("Dữ liệu ảnh không hợp lệ (không phải base64 hợp lệ).");
        }

        // ----------------------------------------------------------------
        // 2. Gọi Gemini Vision API
        // ----------------------------------------------------------------
        String rawJsonText;
        try {
            rawJsonText = geminiVisionService.extractFromImage(request.getImageBase64(), mimeType);
        } catch (GeminiVisionService.GeminiApiException e) {
            log.error("Gemini API lỗi: {}", e.getMessage());
            return buildErrorResult("Không thể kết nối dịch vụ AI. Vui lòng thử lại hoặc nhập tay.");
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gọi Gemini: {}", e.getMessage(), e);
            return buildErrorResult("Đã xảy ra lỗi khi phân tích ảnh. Vui lòng thử lại.");
        }

        // ----------------------------------------------------------------
        // 3. Parse JSON từ Gemini
        // ----------------------------------------------------------------
        GeminiExtractedData extractedData;
        try {
            // Làm sạch response phòng trường hợp Gemini vẫn trả markdown code fence
            String cleanedJson = cleanJsonResponse(rawJsonText);
            extractedData = objectMapper.readValue(cleanedJson, GeminiExtractedData.class);
        } catch (Exception e) {
            log.error("Parse JSON từ Gemini thất bại. Raw text: {}", rawJsonText, e);
            return OcrReceiptResultDTO.builder()
                    .warningMessage("AI không trả về dữ liệu hợp lệ. Vui lòng chụp lại hoặc nhập tay.")
                    .rawModelText(rawJsonText)
                    .overallConfidence(0.0)
                    .items(Collections.emptyList())
                    .build();
        }

        // Cảnh báo khi confidence thấp
        String warningMessage = null;
        if (extractedData.getOverallConfidence() != null && extractedData.getOverallConfidence() < 0.4) {
            warningMessage = "Chất lượng ảnh thấp hoặc không phải phiếu giao hàng. Vui lòng kiểm tra kỹ thông tin trước khi lưu.";
        }

        // ----------------------------------------------------------------
        // 4. Match Supplier
        // ----------------------------------------------------------------
        OcrReceiptMatchingService.PartnerMatchResult partnerMatch =
                matchingService.matchSupplier(extractedData.getSupplierName());

        // ----------------------------------------------------------------
        // 5. Match từng Product
        // ----------------------------------------------------------------
        List<OcrReceiptItemDTO> items = Collections.emptyList();
        if (extractedData.getItems() != null) {
            items = extractedData.getItems().stream().map(rawItem -> {
                OcrReceiptMatchingService.ProductMatchResult productMatch =
                        matchingService.matchProduct(rawItem.getProductName(), rawItem.getProductCode());

                return OcrReceiptItemDTO.builder()
                        .productNameRaw(rawItem.getProductName())
                        .productCodeRaw(rawItem.getProductCode())
                        .quantity(rawItem.getQuantity())
                        .unitPrice(rawItem.getUnitPrice())
                        .batchNo(rawItem.getBatchNo())
                        .expiryDate(rawItem.getExpiryDate())
                        .serialNumber(rawItem.getSerialNumber())
                        .matchedProductId(productMatch.productId())
                        .matchedProductName(productMatch.productName())
                        .matchedProductCode(productMatch.productCode())
                        .productMatchConfidence(productMatch.confidence())
                        .build();
            }).collect(Collectors.toList());
        }

        // ----------------------------------------------------------------
        // 6. Build kết quả
        // ----------------------------------------------------------------
        return OcrReceiptResultDTO.builder()
                .supplierNameRaw(extractedData.getSupplierName())
                .matchedPartnerId(partnerMatch.partnerId())
                .matchedPartnerName(partnerMatch.partnerName())
                .partnerMatchConfidence(partnerMatch.confidence())
                .notes(extractedData.getNotes())
                .items(items)
                .overallConfidence(extractedData.getOverallConfidence() != null
                        ? extractedData.getOverallConfidence() : 0.0)
                .warningMessage(warningMessage)
                .rawModelText(rawJsonText)
                .build();
    }

    /**
     * Loại bỏ markdown code fence nếu Gemini vẫn trả dù đã dặn không dùng.
     */
    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        return cleaned;
    }

    private OcrReceiptResultDTO buildErrorResult(String message) {
        return OcrReceiptResultDTO.builder()
                .warningMessage(message)
                .overallConfidence(0.0)
                .items(Collections.emptyList())
                .build();
    }

    // ----------------------------------------------------------------
    // Inner POJO để deserialize kết quả JSON từ Gemini
    // ----------------------------------------------------------------

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiExtractedData {
        private String supplierName;
        private String notes;
        private List<GeminiItemData> items;
        private Double overallConfidence;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GeminiItemData {
            private String productName;
            private String productCode;
            private Integer quantity;
            private Double unitPrice;
            private String batchNo;
            private String expiryDate;
            private String serialNumber;
        }
    }
}

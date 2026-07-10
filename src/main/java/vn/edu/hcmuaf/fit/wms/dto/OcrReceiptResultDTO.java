package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO kết quả trả về sau khi OCR ảnh phiếu nhập kho.
 * Chứa thông tin nhà cung cấp và danh sách hàng hoá được trích xuất,
 * cùng với kết quả match tương ứng trong hệ thống.
 * Endpoint /ocr-scan trả về DTO này — KHÔNG ghi DB.
 */
@Getter
@Setter
@Builder
public class OcrReceiptResultDTO {

    // --- Thông tin nhà cung cấp ---
    private String supplierNameRaw;
    private Long matchedPartnerId;
    private String matchedPartnerName;
    private Double partnerMatchConfidence;  // 0.0 – 1.0

    // --- Ghi chú tổng phiếu ---
    private String notes;

    // --- Danh sách hàng hoá ---
    private List<OcrReceiptItemDTO> items;

    // --- Mức độ tin cậy tổng thể (do Gemini tự đánh giá) ---
    private Double overallConfidence;

    // --- Cảnh báo hiển thị cho người dùng (nếu có) ---
    private String warningMessage;

    // --- Raw text từ Gemini (chỉ dùng để debug, không hiển thị mặc định) ---
    private String rawModelText;
}

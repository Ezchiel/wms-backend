package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO nhận ảnh từ frontend để OCR bằng Gemini Vision.
 * imageBase64: chuỗi base64 của ảnh (không kèm data URI prefix).
 * mimeType: MIME type của ảnh, ví dụ "image/jpeg", "image/png", "image/webp".
 */
@Getter
@Setter
public class OcrScanRequestDTO {
    private String imageBase64;
    private String mimeType;
}

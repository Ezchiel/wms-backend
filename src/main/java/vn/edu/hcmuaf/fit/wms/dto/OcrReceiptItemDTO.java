package vn.edu.hcmuaf.fit.wms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO đại diện cho một dòng hàng được OCR trích xuất từ ảnh phiếu nhập kho.
 * Chứa cả thông tin thô từ ảnh lẫn kết quả match với Product trong hệ thống.
 */
@Getter
@Setter
@Builder
public class OcrReceiptItemDTO {

    // --- Dữ liệu thô từ Gemini ---
    private String productNameRaw;
    private String productCodeRaw;
    private Integer quantity;
    private Double unitPrice;
    private String batchNo;
    private String expiryDate;      // định dạng "yyyy-MM-dd" hoặc null
    private String serialNumber;

    // --- Kết quả match với Product trong hệ thống ---
    private Long matchedProductId;
    private String matchedProductName;
    private String matchedProductCode;
    private Double productMatchConfidence;  // 0.0 – 1.0
}

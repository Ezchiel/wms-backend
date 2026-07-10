package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.OcrReceiptResultDTO;
import vn.edu.hcmuaf.fit.wms.dto.OcrScanRequestDTO;

public interface OcrReceiptService {

    /**
     * Nhận ảnh phiếu nhập kho từ frontend, gọi Gemini Vision để OCR,
     * match dữ liệu với Partner/Product trong hệ thống,
     * và trả về DTO gợi ý (KHÔNG ghi DB).
     */
    OcrReceiptResultDTO scanReceipt(OcrScanRequestDTO request);
}

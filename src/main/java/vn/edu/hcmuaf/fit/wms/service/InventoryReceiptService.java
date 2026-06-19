package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelResponseDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;

public interface InventoryReceiptService {
    Page<ReceiptResponseDTO> getAllReceipts(String keyword, ReceiptStatus status, int page, int size, String sortBy,
            String sortDir);

    ReceiptResponseDTO createReceipt(ReceiptRequestDTO requestDTO);

    ReceiptResponseDTO confirmReceipt(Long receiptId);

    CountAndLabelResponseDTO countAndLabel(Long receiptId, Long detailId, CountAndLabelRequestDTO request);
}

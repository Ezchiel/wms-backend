package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CountAndLabelResponseDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;

import java.util.List;

public interface InventoryReceiptService {
    ReceiptResponseDTO createReceipt(ReceiptRequestDTO requestDTO);
    List<ReceiptResponseDTO> getAllReceipts();
    ReceiptResponseDTO confirmReceipt(Long receiptId);
    CountAndLabelResponseDTO countAndLabel(Long receiptId, Long detailId, CountAndLabelRequestDTO request);
}

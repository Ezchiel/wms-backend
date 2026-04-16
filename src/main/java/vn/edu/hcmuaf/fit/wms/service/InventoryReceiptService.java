package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ReceiptResponseDTO;

import java.util.List;

public interface InventoryReceiptService {

    @Transactional
    ReceiptResponseDTO createReceipt(ReceiptRequestDTO requestDTO);

    List<ReceiptResponseDTO> getAllReceipts();

    @Transactional
    ReceiptResponseDTO confirmReceipt(Long receiptId);
}

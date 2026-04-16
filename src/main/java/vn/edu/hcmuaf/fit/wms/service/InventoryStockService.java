package vn.edu.hcmuaf.fit.wms.service;

import jakarta.transaction.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;

public interface InventoryStockService {
    @Transactional
    void addStock(InventoryStockRequestDTO request);
}

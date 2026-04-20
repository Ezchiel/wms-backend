package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;

public interface InventoryStockService {
    void addStock(InventoryStockRequestDTO request, String referenceCode);
    void deductStock(Long productId, Long locationId, Integer quantityToDeduct, String referenceCode);
    void adjustStock(Long productId, Long locationId, String batchNo, Integer actualQuantity, String referenceCode);
    Integer getCurrentStockQuantity(Long productId, Long locationId);
}

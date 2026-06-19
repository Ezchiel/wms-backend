package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockResponseDTO;

import java.util.List;

public interface InventoryStockService {
    void addStock(InventoryStockRequestDTO request, String referenceCode);
    void deductStock(Long productId, Long locationId, Integer quantityToDeduct, String referenceCode);
    void deductStock(Long productId, Long locationId, Integer quantityToDeduct, String batchNo, String serialNumber, String referenceCode);
    void adjustStock(Long productId, Long locationId, String batchNo, Integer actualQuantity, String referenceCode);

    Integer getCurrentStockQuantity(Long productId, Long locationId);
    Integer getCurrentStockQuantityByBatch(Long productId, Long locationId, String batchNo);

    List<InventoryStockResponseDTO> getAllStocks();
    InventoryStockResponseDTO getStockById(Long id);
    List<InventoryStockResponseDTO> getStocksByProductId(Long productId);
    List<InventoryStockResponseDTO> getStocksByLocationId(Long locationId);
    List<InventoryStockResponseDTO> getStockByLocationIdAndProductId(Long locationId, Long productId);
}

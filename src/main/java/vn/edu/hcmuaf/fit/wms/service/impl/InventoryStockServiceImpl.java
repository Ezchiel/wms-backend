package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.repository.InventoryStockRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl implements InventoryStockService {

    private final InventoryStockRepository stockRepository;

    @Override
    public void addStock(InventoryStockRequestDTO request) {
        Product productRef = new Product();
        productRef.setId(request.getProductId());

        StorageLocation locationRef = new StorageLocation();
        locationRef.setId(request.getLocationId());

        // Case 1: serial number
        if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()) {
            if (stockRepository.existsBySerialNumber(request.getSerialNumber())) {
                throw new IllegalArgumentException("Serial Number " + request.getSerialNumber() + " đã tồn tại trong hệ thống!");
            }

            InventoryStock newStock = InventoryStock.builder()
                    .product(productRef)
                    .location(locationRef)
                    .quantity(1)
                    .batchNo(request.getBatchNo())
                    .expiryDate(request.getExpiryDate())
                    .serialNumber(request.getSerialNumber())
                    .build();
            stockRepository.save(newStock);
            return;
        }

        // Case 2: Batch no
        Optional<InventoryStock> existingStockOpt;
        if (request.getBatchNo() != null && !request.getBatchNo().isBlank()) {
            existingStockOpt = stockRepository.findFirstByProduct_IdAndLocation_IdAndBatchNo(
                    request.getProductId(), request.getLocationId(), request.getBatchNo());
        } else {
            existingStockOpt = stockRepository.findFirstByProduct_IdAndLocation_IdAndBatchNoIsNull(
                    request.getProductId(), request.getLocationId());
        }

        // add up or create
        if (existingStockOpt.isPresent()) {
            InventoryStock stock = existingStockOpt.get();
            stock.setQuantity(stock.getQuantity() + request.getQuantity());

            if (request.getExpiryDate() != null) {
                stock.setExpiryDate(request.getExpiryDate());
            }
            stockRepository.save(stock);

        } else {
            InventoryStock newStock = InventoryStock.builder()
                    .product(productRef)
                    .location(locationRef)
                    .quantity(request.getQuantity())
                    .batchNo(request.getBatchNo())
                    .expiryDate(request.getExpiryDate())
                    .build();
            stockRepository.save(newStock);
        }
    }
}

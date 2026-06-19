package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.entity.InventoryTransaction;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.TransactionType;
import vn.edu.hcmuaf.fit.wms.repository.InventoryStockRepository;
import vn.edu.hcmuaf.fit.wms.repository.InventoryTransactionRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl implements InventoryStockService {

    private final InventoryStockRepository stockRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;

    @Override
    @Transactional
    public void addStock(InventoryStockRequestDTO request, String referenceCode) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        StorageLocation location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Vị trí không tồn tại"));

        // Case 1: serial number
        if (request.getSerialNumber() != null && !request.getSerialNumber().trim().isEmpty()) {
            if (stockRepository.existsBySerialNumber(request.getSerialNumber().trim())) {
                throw new RuntimeException("Lỗi: Số Serial " + request.getSerialNumber() + " này đã được kiểm đếm hoặc đã tồn tại trong hệ thống!");
            }
            InventoryStock newStock = InventoryStock.builder()
                    .product(product)
                    .location(location)
                    .quantity(request.getQuantity())
                    .batchNo(request.getBatchNo())
                    .serialNumber(request.getSerialNumber())
                    .expiryDate(request.getExpiryDate())
                    .build();
            stockRepository.save(newStock);

            logTransaction(request.getProductId(), request.getLocationId(), TransactionType.RECEIPT, referenceCode, request.getQuantity());

            return;
        }

        // Case 2: Batch no
        Optional<InventoryStock> existingStockOpt;
        if (request.getBatchNo() != null && !request.getBatchNo().trim().isEmpty()) {
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
                    .product(product)
                    .location(location)
                    .quantity(request.getQuantity())
                    .batchNo(request.getBatchNo())
                    .expiryDate(request.getExpiryDate())
                    .build();
            stockRepository.save(newStock);
        }

        logTransaction(request.getProductId(), request.getLocationId(), TransactionType.RECEIPT, referenceCode, request.getQuantity());
    }

    @Override
    @Transactional
    public void deductStock(Long productId, Long locationId, Integer quantityToDeduct, String referenceCode) {
        List<InventoryStock> stocks = stockRepository.findByProduct_IdAndLocation_IdWithLock(productId, locationId);

        int remainingToDeduct = quantityToDeduct;

        for (InventoryStock stock : stocks) {
            if (remainingToDeduct <= 0) break;

            int currentQty = stock.getQuantity();
            if (currentQty <= 0) continue;

            int deductAmount = Math.min(currentQty, remainingToDeduct);

            // update quantity of current stock
            stock.setQuantity(currentQty - deductAmount);
            remainingToDeduct -= deductAmount;

            // if quantity = 0 -> delete stock
            if (stock.getQuantity() == 0) {
                stockRepository.delete(stock);
                checkAndUpdateLocationIsFull(locationId);
            } else {
                stockRepository.save(stock);
            }
        }

        if (remainingToDeduct > 0) {
            throw new RuntimeException("Không đủ số lượng tồn kho để xuất cho sản phẩm có ID: " + productId);
        }

        logTransaction(productId, locationId, TransactionType.ISSUE, referenceCode, quantityToDeduct);
    }

    @Override
    @Transactional
    public void deductStock(Long productId, Long locationId, Integer quantityToDeduct, String batchNo, String serialNumber, String referenceCode) {
        boolean hasBatch = batchNo != null && !batchNo.trim().isEmpty();
        boolean hasSerial = serialNumber != null && !serialNumber.trim().isEmpty();

        List<InventoryStock> stocks;
        if (hasBatch && hasSerial) {
            stocks = stockRepository.findByProduct_IdAndLocation_IdAndBatchNoAndSerialNumberWithLock(productId, locationId, batchNo, serialNumber);
        } else if (hasBatch) {
            stocks = stockRepository.findByProduct_IdAndLocation_IdAndBatchNoWithLock(productId, locationId, batchNo);
        } else if (hasSerial) {
            stocks = stockRepository.findByProduct_IdAndLocation_IdAndBatchNoIsNullAndSerialNumberWithLock(productId, locationId, serialNumber);
        } else {
            stocks = stockRepository.findByProduct_IdAndLocation_IdWithLock(productId, locationId);
        }

        int remainingToDeduct = quantityToDeduct;

        for (InventoryStock stock : stocks) {
            if (remainingToDeduct <= 0) break;

            int currentQty = stock.getQuantity();
            if (currentQty <= 0) continue;

            int deductAmount = Math.min(currentQty, remainingToDeduct);

            // update quantity of current stock
            stock.setQuantity(currentQty - deductAmount);
            remainingToDeduct -= deductAmount;

            // if quantity = 0 -> delete stock
            if (stock.getQuantity() == 0) {
                stockRepository.delete(stock);
                checkAndUpdateLocationIsFull(locationId);
            } else {
                stockRepository.save(stock);
            }
        }

        if (remainingToDeduct > 0) {
            throw new RuntimeException("Không đủ số lượng tồn kho để xuất cho sản phẩm có ID: " + productId +
                    (hasBatch ? " với số lô: " + batchNo : "") + (hasSerial ? " với số serial: " + serialNumber : ""));
        }

        logTransaction(productId, locationId, TransactionType.ISSUE, referenceCode, quantityToDeduct);
    }

    @Override
    @Transactional
    public void adjustStock(Long productId, Long locationId, String batchNo, Integer actualQuantity, String referenceCode) {
        InventoryStock stock;

        if (batchNo != null && !batchNo.isBlank()) {
            stock = stockRepository
                    .findFirstByProduct_IdAndLocation_IdAndBatchNo(productId, locationId, batchNo)
                    .orElseGet(() -> buildNewStock(productId, locationId, batchNo));
        } else {
            stock = stockRepository
                    .findFirstByProduct_IdAndLocation_IdAndBatchNoIsNull(productId, locationId)
                    .orElseGet(() -> buildNewStock(productId, locationId, null));
        }

        int currentQty = stock.getQuantity();
        int variance = actualQuantity - currentQty;

        if (variance == 0) return;

        stock.setQuantity(actualQuantity);

        if (actualQuantity == 0 && stock.getId() != null) {
            stockRepository.delete(stock);
            checkAndUpdateLocationIsFull(locationId);
        } else {
            stockRepository.save(stock);
        }

        logTransaction(productId, locationId, TransactionType.ADJUST, referenceCode, variance);
    }

    /**
     * Get the total inventory at a given location (regardless of batch).
     */
    @Override
    public Integer getCurrentStockQuantity(Long productId, Long locationId) {
        List<InventoryStock> stocks = stockRepository.findByProduct_IdAndLocation_Id(productId, locationId);
        return stocks.stream()
                .mapToInt(InventoryStock::getQuantity)
                .sum();
    }

    /**
     * Get inventory by batch. If batchNo = null, return the total quantity.
     */
    @Override
    public Integer getCurrentStockQuantityByBatch(Long productId, Long locationId, String batchNo) {
        if (batchNo != null && !batchNo.isBlank()) {
            return stockRepository
                    .findFirstByProduct_IdAndLocation_IdAndBatchNo(productId, locationId, batchNo)
                    .map(InventoryStock::getQuantity)
                    .orElse(0);
        }
        return getCurrentStockQuantity(productId, locationId);
    }

    @Override
    public List<InventoryStockResponseDTO> getAllStocks() {
        return stockRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryStockResponseDTO getStockById(Long id) {
        InventoryStock stock = stockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho với ID: " + id));
        return mapToResponseDTO(stock);
    }

    @Override
    public List<InventoryStockResponseDTO> getStocksByProductId(Long productId) {
        return stockRepository.findByProductId(productId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryStockResponseDTO> getStocksByLocationId(Long locationId) {
        return stockRepository.findByLocation_Id(locationId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryStockResponseDTO> getStockByLocationIdAndProductId(Long locationId, Long productId) {
        return stockRepository.findByLocation_IdAndProduct_Id(locationId, productId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // ======================== PRIVATE HELPERS ========================
    private void checkAndUpdateLocationIsFull(Long locationId) {
        boolean hasOtherStock = stockRepository.findByLocation_Id(locationId)
                .stream().anyMatch(s -> s.getQuantity() > 0);
        if (!hasOtherStock) {
            StorageLocation loc = locationRepository.findById(locationId)
                    .orElseThrow(() -> new RuntimeException("Vị trí không tồn tại: " + locationId));
            if (loc.isFull()) {
                loc.setFull(false);
                locationRepository.save(loc);
            }
        }
    }

    private InventoryStock buildNewStock(Long productId, Long locationId, String batchNo) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + productId));
        StorageLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Vị trí không tồn tại: " + locationId));

        return InventoryStock.builder()
                .product(product)
                .location(location)
                .quantity(0)
                .batchNo(batchNo)
                .build();
    }

    private void logTransaction(Long productId, Long locationId, TransactionType type, String refCode, Integer qty) {
        Product productRef = new Product();
        productRef.setId(productId);

        StorageLocation locationRef = new StorageLocation();
        locationRef.setId(locationId);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(productRef)
                .location(locationRef)
                .transactionType(type)
                .referenceCode(refCode)
                .quantity(qty)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);
    }

    private InventoryStockResponseDTO mapToResponseDTO(InventoryStock stock) {
        return InventoryStockResponseDTO.builder()
                .id(stock.getId())
                .productId(stock.getProduct().getId())
                .productName(stock.getProduct().getProductName())
                .locationId(stock.getLocation().getId())
                .quantity(stock.getQuantity())
                .batchNo(stock.getBatchNo())
                .expiryDate(stock.getExpiryDate())
                .serialNumber(stock.getSerialNumber())
                .build();
    }
}

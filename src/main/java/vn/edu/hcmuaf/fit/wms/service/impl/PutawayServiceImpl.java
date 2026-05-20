package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawayRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawaySuggestionDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.entity.enums.LpnStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.repository.*;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;
import vn.edu.hcmuaf.fit.wms.service.PutawayService;

@Service
@RequiredArgsConstructor
public class PutawayServiceImpl implements PutawayService {

    private final LpnRepository lpnRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryReceiptDetailRepository inventoryReceiptDetailRepository;
    private final InventoryReceiptRepository receiptRepository;

    @Override
    @Transactional(readOnly = true)
    public PutawaySuggestionDTO getSuggestion(String lpnCode) {
        Lpn lpn = lpnRepository.findByLpnCode(lpnCode)
                .orElseThrow(() -> new RuntimeException("Mã LPN không hợp lệ"));

        Long productId = lpn.getProduct().getId();

        StorageLocation targetLocation = storageLocationRepository.findOptimalLocationForPutaway(productId)
                .orElseThrow(() -> new RuntimeException("Kho đã đầy, không tìm thấy vị trí cất hàng!"));

        return PutawaySuggestionDTO.builder()
                .lpnCode(lpnCode)
                .productName(lpn.getProduct().getProductName())
                .suggestedLocationCode(targetLocation.getBarcode())
                .suggestedLocationId(targetLocation.getId())
                .build();
    }

    @Override
    @Transactional
    public void confirm(PutawayRequestDTO request) {
        Lpn lpn = lpnRepository.findByLpnCode(request.getLpnCode())
                .orElseThrow(() -> new RuntimeException("Mã LPN không hợp lệ"));

        if (lpn.getStatus() == LpnStatus.STORED) {
            throw new RuntimeException("LPN này đã được cất vào kho trước đó!");
        }

        // Get target location
        StorageLocation targetLocation = storageLocationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Vị trí cất hàng không hợp lệ"));

        // Get stage location
        StorageLocation stagingLocation = storageLocationRepository.findFirstByLocationType(LocationType.RECEIVING_DOCK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bãi nhận hàng tạm"));

        // Subtract stock at the stage
        InventoryStock stagingStock = inventoryStockRepository
                .findByProductAndLocationAndBatchNoAndSerialNumber(lpn.getProduct(), stagingLocation, lpn.getBatchNo(), lpn.getSerialNumber())
                .orElseThrow(() -> new RuntimeException("Lỗi logic: Không tìm thấy tồn kho tạm của sản phẩm này"));

        if (stagingStock.getQuantity() < lpn.getQuantity()) {
            throw new RuntimeException("Số lượng tồn kho tạm không đủ để thực hiện cất hàng");
        }
        stagingStock.setQuantity(stagingStock.getQuantity() - lpn.getQuantity());
        inventoryStockRepository.save(stagingStock);

        // Add stock
        InventoryStock targetStock = inventoryStockRepository
                .findByProductAndLocationAndBatchNoAndSerialNumber(lpn.getProduct(), targetLocation, lpn.getBatchNo(), lpn.getSerialNumber())
                .orElse(InventoryStock.builder()
                        .product(lpn.getProduct())
                        .location(targetLocation)
                        .quantity(0)
                        .batchNo(lpn.getBatchNo())
                        .expiryDate(lpn.getExpiryDate())
                        .serialNumber(lpn.getSerialNumber())
                        .build());

        targetStock.setQuantity(targetStock.getQuantity() + lpn.getQuantity());
        inventoryStockRepository.save(targetStock);

        // Update LPN status
        lpn.setStatus(LpnStatus.STORED);
        lpnRepository.save(lpn);

        // Update receipt status
        InventoryReceipt receipt = lpn.getReceipt();
        boolean allDone = lpnRepository.findAllByReceipt_Id(receipt.getId()).stream()
                .allMatch(l -> l.getStatus() == LpnStatus.STORED);
        if (allDone) {
            receipt.setStatus(ReceiptStatus.COMPLETED);
            receiptRepository.save(receipt);
        }

        // Update location into Receipt Detail
        InventoryReceiptDetail detail = lpn.getReceiptDetail();
        if (detail != null) {
            detail.setLocation(targetLocation);
            inventoryReceiptDetailRepository.save(detail);
        }
    }
}

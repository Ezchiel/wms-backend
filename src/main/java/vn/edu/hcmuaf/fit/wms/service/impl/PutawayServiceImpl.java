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
    private final InventoryStockService stockService;

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

        if (targetLocation.getLocationType() != LocationType.STORAGE) {
            throw new RuntimeException(
                    "Vị trí cất hàng phải là loại STORAGE! Vị trí \"" + targetLocation.getBarcode()
                    + "\" có loại: " + targetLocation.getLocationType()
            );
        }

        // Get stage location
        StorageLocation stagingLocation = storageLocationRepository.findFirstByLocationType(LocationType.RECEIVING_DOCK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bãi nhận hàng tạm"));

        // Trừ tồn kho tạm tại staging location (Dock nhận hàng) và ghi nhận giao dịch ISSUE
        stockService.deductStock(
                lpn.getProduct().getId(),
                stagingLocation.getId(),
                lpn.getQuantity(),
                lpn.getBatchNo(),
                lpn.getSerialNumber(),
                "PUTAWAY-" + lpn.getLpnCode()
        );

        // Thêm tồn kho vào vị trí đích (STORAGE) và ghi nhận giao dịch RECEIPT
        stockService.addStock(
                InventoryStockRequestDTO.builder()
                        .productId(lpn.getProduct().getId())
                        .locationId(targetLocation.getId())
                        .quantity(lpn.getQuantity())
                        .batchNo(lpn.getBatchNo())
                        .expiryDate(lpn.getExpiryDate())
                        .serialNumber(lpn.getSerialNumber())
                        .build(),
                "PUTAWAY-" + lpn.getLpnCode()
        );

        // Cập nhật trạng thái vị trí đích
        targetLocation.setFull(true);
        storageLocationRepository.save(targetLocation);

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

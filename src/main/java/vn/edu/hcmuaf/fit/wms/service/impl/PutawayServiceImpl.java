package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawayRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawaySuggestionDTO;
import vn.edu.hcmuaf.fit.wms.entity.Lpn;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LpnStatus;
import vn.edu.hcmuaf.fit.wms.repository.LpnRepository;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;
import vn.edu.hcmuaf.fit.wms.service.PutawayService;

@Service
@RequiredArgsConstructor
public class PutawayServiceImpl implements PutawayService {

    private final LpnRepository lpnRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryStockService inventoryStockService;

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

        StorageLocation location = storageLocationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Vị trí cất hàng không hợp lệ"));

        InventoryStockRequestDTO stockRequest = InventoryStockRequestDTO.builder()
                .productId(lpn.getProduct().getId())
                .locationId(location.getId())
                .quantity(lpn.getQuantity())
                .batchNo(lpn.getBatchNo())
                .expiryDate(lpn.getExpiryDate())
                .build();

        inventoryStockService.addStock(stockRequest, lpn.getLpnCode());

        lpn.setStatus(LpnStatus.STORED);
        lpnRepository.save(lpn);

        // Logic for location isFull
    }
}

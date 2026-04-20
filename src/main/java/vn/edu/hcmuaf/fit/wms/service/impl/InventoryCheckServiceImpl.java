package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.CheckRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CheckResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;
import vn.edu.hcmuaf.fit.wms.repository.InventoryCheckRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryCheckService;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryCheckServiceImpl implements InventoryCheckService {

    private final InventoryCheckRepository checkRepository;
    private final InventoryStockService stockService;

    @Override
    public List<CheckResponseDTO> getAllChecks() {
        return checkRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CheckResponseDTO createCheck(CheckRequestDTO requestDTO) {
        InventoryCheck check = InventoryCheck.builder()
                .checkCode("PKK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .checkDate(LocalDateTime.now())
                .status(CheckStatus.PENDING)
                .notes(requestDTO.getNotes())
                .build();

        List<InventoryCheckDetail> details = requestDTO.getDetails().stream().map(dto -> {
            Integer systemQty = stockService.getCurrentStockQuantity(dto.getProductId(), dto.getLocationId());
            Integer variance = dto.getActualQuantity() - systemQty;

            Product productRef = new Product();
            productRef.setId(dto.getProductId());
            StorageLocation locationRef = new StorageLocation();
            locationRef.setId(dto.getLocationId());

            return InventoryCheckDetail.builder()
                    .inventoryCheck(check)
                    .product(productRef)
                    .location(locationRef)
                    .systemQuantity(systemQty)
                    .actualQuantity(dto.getActualQuantity())
                    .variance(variance)
                    .reason(dto.getReason())
                    .build();
        }).collect(Collectors.toList());

        check.setDetails(details);
        checkRepository.save(check);

        return mapToDTO(check);
    }

    @Override
    @Transactional
    public CheckResponseDTO confirmCheck(Long checkId) {
        InventoryCheck check = checkRepository.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu kiểm kê!"));

        if (check.getStatus() != CheckStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu đang PENDING!");
        }

        for (InventoryCheckDetail detail : check.getDetails()) {
            if (detail.getVariance() != 0) {
                stockService.adjustStock(
                        detail.getProduct().getId(),
                        detail.getLocation().getId(),
                        null,
                        detail.getActualQuantity(),
                        check.getCheckCode()
                );
            }
        }

        check.setStatus(CheckStatus.COMPLETED);
        checkRepository.save(check);

        return mapToDTO(check);
    }

    private CheckResponseDTO mapToDTO(InventoryCheck entity) {
        if (entity == null) return null;

        return CheckResponseDTO.builder()
                .checkCode(entity.getCheckCode())
                .checkDate(entity.getCheckDate())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .details(entity.getDetails().stream()
                        .map(detail -> CheckResponseDTO.CheckDetailResponseDTO.builder()
                                .productId(detail.getProduct().getId())
                                .locationId(detail.getLocation().getId())
                                .systemQuantity(detail.getSystemQuantity())
                                .actualQuantity(detail.getActualQuantity())
                                .variance(detail.getVariance())
                                .reason(detail.getReason())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

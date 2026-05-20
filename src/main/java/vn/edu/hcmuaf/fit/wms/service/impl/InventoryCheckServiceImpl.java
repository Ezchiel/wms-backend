package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.CheckRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CheckResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;
import vn.edu.hcmuaf.fit.wms.repository.InventoryCheckRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryCheckService;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryCheckServiceImpl implements InventoryCheckService {

    private final InventoryCheckRepository checkRepository;
    private final InventoryStockService stockService;
    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;

    @Override
    public Page<CheckResponseDTO> getAllChecks(
            String keyword, CheckStatus status,
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        return checkRepository.searchChecks(keyword, status, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public CheckResponseDTO getCheckById(Long id) {
        InventoryCheck check = findCheckOrThrow(id);
        return mapToDTO(check);
    }

    @Override
    @Transactional
    public CheckResponseDTO createCheck(CheckRequestDTO requestDTO) {
        if (requestDTO.getDetails() == null || requestDTO.getDetails().isEmpty()) {
            throw new RuntimeException("Phiếu kiểm kê phải có ít nhất 1 sản phẩm cần kiểm!");
        }

        String currentUser = Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getName();

        InventoryCheck check = InventoryCheck.builder()
                .checkCode("PKK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .checkDate(LocalDateTime.now())
                .status(CheckStatus.PENDING)
                .notes(requestDTO.getNotes())
                .createdBy(currentUser)
                .build();

        List<InventoryCheckDetail> details = requestDTO.getDetails().stream().map(dto -> {
            // Validate product & location exist
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy sản phẩm với ID: " + dto.getProductId()));

            StorageLocation location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy vị trí với ID: " + dto.getLocationId()));

            // Get system quantity — supports both batch and non-batch
            Integer systemQty = stockService.getCurrentStockQuantityByBatch(
                    dto.getProductId(), dto.getLocationId(), dto.getBatchNo());

            int variance = dto.getActualQuantity() - systemQty;

            return InventoryCheckDetail.builder()
                    .inventoryCheck(check)
                    .product(product)
                    .location(location)
                    .batchNo(dto.getBatchNo())
                    .systemQuantity(systemQty)
                    .actualQuantity(dto.getActualQuantity())
                    .variance(variance)
                    .reason(dto.getReason())
                    .build();
        }).collect(Collectors.toList());

        check.setDetails(details);
        InventoryCheck saved = checkRepository.save(check);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public CheckResponseDTO confirmCheck(Long checkId) {
        InventoryCheck check = findCheckOrThrow(checkId);

        if (check.getStatus() != CheckStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu đang ở trạng thái PENDING!");
        }

        // Adjust inventory for items with discrepancies
        for (InventoryCheckDetail detail : check.getDetails()) {
            if (detail.getVariance() != 0) {
                stockService.adjustStock(
                        detail.getProduct().getId(),
                        detail.getLocation().getId(),
                        detail.getBatchNo(),
                        detail.getActualQuantity(),
                        check.getCheckCode()
                );
            }
        }

        check.setStatus(CheckStatus.COMPLETED);
        checkRepository.save(check);

        return mapToDTO(check);
    }

    @Override
    @Transactional
    public CheckResponseDTO cancelCheck(Long checkId) {
        InventoryCheck check = findCheckOrThrow(checkId);

        if (check.getStatus() != CheckStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể huỷ phiếu đang ở trạng thái PENDING!");
        }

        check.setStatus(CheckStatus.CANCELLED);
        checkRepository.save(check);

        return mapToDTO(check);
    }

    // ======================== PRIVATE HELPERS ========================

    private InventoryCheck findCheckOrThrow(Long id) {
        return checkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu kiểm kê với ID: " + id));
    }

    private CheckResponseDTO mapToDTO(InventoryCheck entity) {
        if (entity == null) return null;

        List<CheckResponseDTO.CheckDetailResponseDTO> detailDTOs = entity.getDetails() == null
                ? List.of()
                : entity.getDetails().stream()
                .map(d -> CheckResponseDTO.CheckDetailResponseDTO.builder()
                        .id(d.getId())
                        .productId(d.getProduct().getId())
                        .productName(d.getProduct().getProductName())
                        .locationId(d.getLocation().getId())
                        .locationBarcode(d.getLocation().getBarcode())
                        .batchNo(d.getBatchNo())
                        .systemQuantity(d.getSystemQuantity())
                        .actualQuantity(d.getActualQuantity())
                        .variance(d.getVariance())
                        .reason(d.getReason())
                        .build())
                .collect(Collectors.toList());

        return CheckResponseDTO.builder()
                .id(entity.getId())
                .checkCode(entity.getCheckCode())
                .checkDate(entity.getCheckDate())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .details(detailDTOs)
                .build();
    }
}
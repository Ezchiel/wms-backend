package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.PickingTaskStatus;
import vn.edu.hcmuaf.fit.wms.repository.InventoryIssueRepository;
import vn.edu.hcmuaf.fit.wms.repository.PartnerRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.repository.PickingTaskRepository;
import vn.edu.hcmuaf.fit.wms.service.InventoryIssueService;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryIssueServiceImpl implements InventoryIssueService {

    private final InventoryIssueRepository issueRepository;
    private final PartnerRepository partnerRepository;
    private final InventoryStockService stockService;
    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;
    private final PickingTaskRepository pickingTaskRepository;

    @Override
    public Page<IssueResponseDTO> getAllIssues(String keyword, IssueStatus status,
                                               int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;

        return issueRepository.searchIssues(kw, status, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public IssueResponseDTO getIssueById(Long id) {
        InventoryIssue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất kho với id: " + id));
        return mapToDTO(issue);
    }

    @Override
    @Transactional
    public IssueResponseDTO createIssue(IssueRequestDTO requestDTO) {
        Partner customer = partnerRepository.findById(requestDTO.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng!"));

        // Validate tồn kho trước khi tạo phiếu
        if (requestDTO.getDetails() != null) {
            for (IssueRequestDTO.IssueDetailDTO dto : requestDTO.getDetails()) {
                if (dto.getLocationId() != null && dto.getLocationId().equals(1L)) {
                    throw new RuntimeException("Không được phép xuất kho từ Khu vực nhận hàng mặc định của hệ thống!");
                }
                Integer available = stockService.getCurrentStockQuantity(dto.getProductId(), dto.getLocationId());
                if (available == null || available < dto.getQuantity()) {
                    Product product = productRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
                    throw new RuntimeException(
                            "Không đủ tồn kho cho sản phẩm \"" + product.getProductName()
                            + "\" tại vị trí đã chọn. Hiện có: " + (available != null ? available : 0)
                            + ", yêu cầu: " + dto.getQuantity()
                    );
                }
            }
        }

        String createdBy = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "system";

        InventoryIssue issue = InventoryIssue.builder()
                .issueCode("PXK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .issueDate(LocalDateTime.now())
                .status(IssueStatus.DRAFT)
                .notes(requestDTO.getNotes())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        List<InventoryIssueDetail> details = requestDTO.getDetails().stream().map(dto -> {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            StorageLocation location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Vị trí không tồn tại"));

            return InventoryIssueDetail.builder()
                    .inventoryIssue(issue)
                    .product(product)
                    .location(location)
                    .quantity(dto.getQuantity())
                    .build();
        }).collect(Collectors.toList());

        issue.setDetails(details);
        issueRepository.save(issue);

        return mapToDTO(issue);
    }

    @Override
    @Transactional
    public IssueResponseDTO approveIssue(Long issueId) {
        InventoryIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất kho!"));

        if (issue.getStatus() != IssueStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể duyệt phiếu đang ở trạng thái DRAFT!");
        }

        // Create PickingTask
        List<PickingTask> tasks = issue.getDetails().stream().map(detail ->
            PickingTask.builder()
                    .inventoryIssue(issue)
                    .issueDetail(detail)
                    .product(detail.getProduct())
                    .location(detail.getLocation())
                    .requiredQuantity(detail.getQuantity())
                    .pickedQuantity(0)
                    .status(PickingTaskStatus.PENDING)
                    .build()
        ).collect(Collectors.toList());

        pickingTaskRepository.saveAll(tasks);

        issue.setStatus(IssueStatus.PICKING);
        issueRepository.save(issue);

        return mapToDTO(issue);
    }

    @Override
    @Transactional
    public IssueResponseDTO confirmIssue(Long issueId) {
        InventoryIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất kho!"));

        if (issue.getStatus() != IssueStatus.PICKING) {
            throw new RuntimeException("Chỉ có thể xác nhận xuất hàng với phiếu đang ở trạng thái PICKING!");
        }

        for (InventoryIssueDetail detail : issue.getDetails()) {
            stockService.deductStock(
                    detail.getProduct().getId(),
                    detail.getLocation().getId(),
                    detail.getQuantity(),
                    issue.getIssueCode()
            );
        }

        issue.setStatus(IssueStatus.COMPLETED);
        issueRepository.save(issue);

        return mapToDTO(issue);
    }

    @Override
    @Transactional
    public IssueResponseDTO cancelIssue(Long issueId) {
        InventoryIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất kho!"));

        if (issue.getStatus() != IssueStatus.DRAFT 
                && issue.getStatus() != IssueStatus.PICKING) {
            throw new RuntimeException("Chỉ có thể huỷ phiếu đang ở trạng thái DRAFT hoặc PICKING!");
        }

        // Nếu phiếu đang ở trạng thái PICKING, huỷ luôn các picking task liên quan chưa hoàn thành
        if (issue.getStatus() == IssueStatus.PICKING) {
            List<PickingTask> tasks = pickingTaskRepository.findByInventoryIssue_Id(issueId);
            for (PickingTask task : tasks) {
                if (task.getStatus() == PickingTaskStatus.PENDING || task.getStatus() == PickingTaskStatus.IN_PROGRESS) {
                    task.setStatus(PickingTaskStatus.FAILED);
                    task.setNote("Hủy do phiếu xuất kho bị hủy");
                    pickingTaskRepository.save(task);
                }
            }
        }

        issue.setStatus(IssueStatus.CANCELLED);
        issueRepository.save(issue);

        return mapToDTO(issue);
    }

    private IssueResponseDTO mapToDTO(InventoryIssue entity) {
        if (entity == null) return null;

        List<IssueResponseDTO.IssueDetailResponseDTO> detailDTOs = entity.getDetails() != null
                ? entity.getDetails().stream()
                        .map(detail -> IssueResponseDTO.IssueDetailResponseDTO.builder()
                                .id(detail.getId())
                                .productId(detail.getProduct().getId())
                                .productName(detail.getProduct().getProductName())
                                .productCode(detail.getProduct().getProductCode())
                                .locationId(detail.getLocation().getId())
                                .locationBarcode(detail.getLocation().getBarcode())
                                .locationDescription(detail.getLocation().getDescription())
                                .quantity(detail.getQuantity())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return IssueResponseDTO.builder()
                .id(entity.getId())
                .issueCode(entity.getIssueCode())
                .customerId(entity.getCustomer().getId())
                .customerName(entity.getCustomer().getName())
                .issueDate(entity.getIssueDate())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .details(detailDTOs)
                .build();
    }
}

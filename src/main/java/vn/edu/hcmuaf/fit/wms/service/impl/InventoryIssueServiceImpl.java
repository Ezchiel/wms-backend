package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.repository.InventoryIssueRepository;
import vn.edu.hcmuaf.fit.wms.repository.PartnerRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
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

    @Override
    public List<IssueResponseDTO> getAllIssues() {
        return issueRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IssueResponseDTO createIssue(IssueRequestDTO requestDTO) {
        Partner customer = partnerRepository.findById(requestDTO.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Khách hàng!"));

        InventoryIssue issue = InventoryIssue.builder()
                .issueCode("PXK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .issueDate(LocalDateTime.now())
                .status(ReceiptStatus.PENDING)
                .notes(requestDTO.getNotes())
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
    public IssueResponseDTO confirmIssue(Long issueId) {
        InventoryIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất kho!"));

        if (issue.getStatus() != ReceiptStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu đang ở trạng thái PENDING!");
        }

        for (InventoryIssueDetail detail : issue.getDetails()) {
            stockService.deductStock(
                    detail.getProduct().getId(),
                    detail.getLocation().getId(),
                    detail.getQuantity(),
                    issue.getIssueCode()
            );
        }

        issue.setStatus(ReceiptStatus.COMPLETED);
        issueRepository.save(issue);

        return mapToDTO(issue);
    }

    private IssueResponseDTO mapToDTO(InventoryIssue entity) {
        if (entity == null) return null;

        return IssueResponseDTO.builder()
                .issueCode(entity.getIssueCode())
                .customerName(entity.getCustomer().getName())
                .issueDate(entity.getIssueDate())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .details(entity.getDetails().stream()
                        .map(detail -> IssueResponseDTO.IssueDetailResponseDTO.builder()
                                    .productName(detail.getProduct().getProductName())
                                    .locationDescription(detail.getLocation().getDescription())
                                    .quantity(detail.getQuantity())
                                    .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

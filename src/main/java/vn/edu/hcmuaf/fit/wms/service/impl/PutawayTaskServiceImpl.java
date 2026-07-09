package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.InventoryStockRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawayTaskConfirmRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawayTaskResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.entity.enums.LpnStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.PutawayTaskStatus;
import vn.edu.hcmuaf.fit.wms.entity.enums.ReceiptStatus;
import vn.edu.hcmuaf.fit.wms.repository.*;
import vn.edu.hcmuaf.fit.wms.service.InventoryStockService;
import vn.edu.hcmuaf.fit.wms.service.PutawayTaskService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PutawayTaskServiceImpl implements PutawayTaskService {

    private final PutawayTaskRepository putawayTaskRepository;
    private final LpnRepository lpnRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryReceiptRepository receiptRepository;
    private final InventoryReceiptDetailRepository inventoryReceiptDetailRepository;
    private final InventoryStockService stockService;

    @Override
    @Transactional(readOnly = true)
    public Page<PutawayTaskResponseDTO> getAllTasks(PutawayTaskStatus status, String assignedTo, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        // Fetch all and filter in memory for simplicity; for production a custom JPQL query is preferred
        return putawayTaskRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PutawayTaskResponseDTO> getAvailableTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return putawayTaskRepository.findAvailableTasks(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PutawayTaskResponseDTO getTaskById(Long taskId) {
        PutawayTask task = putawayTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task cất hàng với id: " + taskId));
        return mapToDTO(task);
    }

    @Override
    @Transactional
    public PutawayTaskResponseDTO claimTask(Long taskId, String username) {
        // Use pessimistic write lock to prevent race condition
        PutawayTask task = putawayTaskRepository.findByIdWithLock(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task cất hàng!"));

        if (task.getStatus() != PutawayTaskStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể nhận task ở trạng thái PENDING! Trạng thái hiện tại: " + task.getStatus());
        }

        if (task.getAssignedTo() != null) {
            throw new RuntimeException("Task này đã được nhân viên khác nhận: " + task.getAssignedTo());
        }

        task.setAssignedTo(username);
        task.setStatus(PutawayTaskStatus.IN_PROGRESS);
        return mapToDTO(putawayTaskRepository.save(task));
    }

    @Override
    @Transactional
    public PutawayTaskResponseDTO confirmTask(PutawayTaskConfirmRequestDTO request, String username) {
        // Find and lock task
        PutawayTask task = putawayTaskRepository.findByIdWithLock(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task cất hàng!"));

        // Check ownership
        if (!username.equals(task.getAssignedTo())) {
            throw new RuntimeException("Bạn không có quyền xác nhận task này. Task được giao cho: " + task.getAssignedTo());
        }

        if (task.getStatus() == PutawayTaskStatus.DONE) {
            throw new RuntimeException("Task này đã hoàn thành trước đó!");
        }

        // Get LPN and check status
        Lpn lpn = task.getLpn();
        if (lpn.getStatus() == LpnStatus.STORED) {
            throw new RuntimeException("LPN này đã được cất vào kho trước đó!");
        }

        // Get target location
        StorageLocation targetLocation = storageLocationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Vị trí cất hàng không hợp lệ!"));

        if (targetLocation.getLocationType() != LocationType.STORAGE) {
            throw new RuntimeException(
                    "Vị trí cất hàng phải là loại STORAGE! Vị trí \"" + targetLocation.getBarcode()
                    + "\" có loại: " + targetLocation.getLocationType()
            );
        }

        // Get staging (RECEIVING_DOCK) location
        StorageLocation stagingLocation = storageLocationRepository.findFirstByLocationType(LocationType.RECEIVING_DOCK)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bãi nhận hàng tạm (RECEIVING_DOCK)!"));

        // Deduct stock from staging location
        stockService.deductStock(
                lpn.getProduct().getId(),
                stagingLocation.getId(),
                lpn.getQuantity(),
                lpn.getBatchNo(),
                lpn.getSerialNumber(),
                "PUTAWAY-" + lpn.getLpnCode()
        );

        // Add stock to target location
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

        // Update LPN status
        lpn.setStatus(LpnStatus.STORED);
        lpnRepository.save(lpn);

        // Update receipt detail location
        if (lpn.getReceiptDetail() != null) {
            lpn.getReceiptDetail().setLocation(targetLocation);
            inventoryReceiptDetailRepository.save(lpn.getReceiptDetail());
        }

        // Update task
        task.setStatus(PutawayTaskStatus.DONE);
        task.setTargetLocationId(targetLocation.getId());
        task.setPutawayAt(LocalDateTime.now());
        if (request.getNote() != null) {
            task.setNote(request.getNote());
        }
        PutawayTask savedTask = putawayTaskRepository.save(task);

        // Check and complete receipt if all tasks done
        checkAndCompleteReceipt(task.getReceipt().getId());

        return mapToDTO(savedTask);
    }

    @Override
    @Transactional
    public void checkAndCompleteReceipt(Long receiptId) {
        List<PutawayTask> tasks = putawayTaskRepository.findByReceipt_Id(receiptId);
        boolean allDone = !tasks.isEmpty() && tasks.stream()
                .allMatch(t -> t.getStatus() == PutawayTaskStatus.DONE);

        if (allDone) {
            InventoryReceipt receipt = receiptRepository.findById(receiptId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập!"));
            receipt.setStatus(ReceiptStatus.COMPLETED);
            receiptRepository.save(receipt);
        }
    }

    private PutawayTaskResponseDTO mapToDTO(PutawayTask task) {
        if (task == null) return null;

        Lpn lpn = task.getLpn();
        String suggestedLocationCode = null;
        Long suggestedLocationId = null;

        if (lpn != null && lpn.getProduct() != null) {
            var optimal = storageLocationRepository.findOptimalLocationForPutaway(lpn.getProduct().getId());
            if (optimal.isPresent()) {
                suggestedLocationCode = optimal.get().getBarcode();
                suggestedLocationId = optimal.get().getId();
            }
        }

        return PutawayTaskResponseDTO.builder()
                .id(task.getId())
                .receiptId(task.getReceipt() != null ? task.getReceipt().getId() : null)
                .receiptCode(task.getReceipt() != null ? task.getReceipt().getReceiptCode() : null)
                .lpnCode(lpn != null ? lpn.getLpnCode() : null)
                .productId(task.getProduct() != null ? task.getProduct().getId() : null)
                .productName(task.getProduct() != null ? task.getProduct().getProductName() : null)
                .quantity(lpn != null ? lpn.getQuantity() : null)
                .batchNo(lpn != null ? lpn.getBatchNo() : null)
                .status(task.getStatus())
                .assignedTo(task.getAssignedTo())
                .suggestedLocationCode(suggestedLocationCode)
                .suggestedLocationId(suggestedLocationId)
                .targetLocationId(task.getTargetLocationId())
                .note(task.getNote())
                .putawayAt(task.getPutawayAt())
                .createdAt(task.getCreatedAt())
                .build();
    }
}

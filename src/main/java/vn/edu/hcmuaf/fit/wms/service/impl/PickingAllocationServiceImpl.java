package vn.edu.hcmuaf.fit.wms.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.AllocationResultDTO;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.repository.InventoryStockRepository;
import vn.edu.hcmuaf.fit.wms.service.PickingAllocationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Triển khai thuật toán phân bổ vị trí lấy hàng (greedy, FEFO + pathSequence).
 *
 * <p><b>Thuật toán:</b>
 * <ol>
 *   <li>Lấy tất cả InventoryStock của productId với PESSIMISTIC_WRITE lock
 *       (loại trừ RECEIVING_DOCK / SHIPPING_DOCK, quantity > 0).</li>
 *   <li>Sắp xếp: expiryDate ASC NULLS LAST, sau đó pathSequence ASC.</li>
 *   <li>Duyệt tuần tự và phân bổ số lượng cho đến khi đủ requiredQuantity.</li>
 *   <li>Nếu hết danh sách mà vẫn còn thiếu → throw RuntimeException.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PickingAllocationServiceImpl implements PickingAllocationService {

    private final InventoryStockRepository stockRepository;

    @Override
    @Transactional
    public List<AllocationResultDTO> allocate(Long productId, Integer requiredQuantity, String batchNo) {
        // Lấy danh sách tồn kho với Pessimistic Write lock để tránh race condition
        List<InventoryStock> stocks = stockRepository.findStockForAllocationWithLock(productId, batchNo);

        // Sắp xếp FEFO + pathSequence
        stocks.sort(Comparator
                // expiryDate ASC — lô có hạn dùng sớm hơn lấy trước; null (không có hạn) xếp cuối cùng
                .comparing(s -> s.getExpiryDate(), Comparator.nullsLast(Comparator.naturalOrder()))
        );
        // Sắp xếp thứ cấp: pathSequence ASC (trong cùng expiryDate)
        // Dùng cú pháp riêng để tránh ambiguous lambda
        stocks.sort(Comparator
                .<InventoryStock, java.time.LocalDate>comparing(
                        s -> s.getExpiryDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(s -> {
                    Integer seq = s.getLocation().getPathSequence();
                    return seq != null ? seq : Integer.MAX_VALUE;
                })
        );

        List<AllocationResultDTO> results = new ArrayList<>();
        int remaining = requiredQuantity;

        for (InventoryStock stock : stocks) {
            if (remaining <= 0) break;

            int available = stock.getQuantity();
            if (available <= 0) continue;

            int take = Math.min(available, remaining);

            results.add(AllocationResultDTO.builder()
                    .locationId(stock.getLocation().getId())
                    .batchNo(stock.getBatchNo())
                    .expiryDate(stock.getExpiryDate())
                    .quantity(take)
                    .build());

            remaining -= take;
        }

        if (remaining > 0) {
            throw new RuntimeException(
                    "Không đủ tồn kho để phân bổ lấy hàng cho sản phẩm ID=" + productId
                    + ". Còn thiếu: " + remaining
                    + (batchNo != null && !batchNo.isBlank() ? " (lô: " + batchNo + ")" : "")
            );
        }

        return results;
    }
}

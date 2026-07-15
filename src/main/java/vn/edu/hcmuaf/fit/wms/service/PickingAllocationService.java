package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.AllocationResultDTO;

import java.util.List;

/**
 * Service phân bổ vị trí lấy hàng (Picking Allocation).
 *
 * <p>Nhiệm vụ: Với 1 sản phẩm + số lượng cần xuất, tính toán và trả về danh sách
 * các cặp (locationId, batchNo, quantity) để tạo PickingTask, áp dụng nguyên tắc:
 * <ul>
 *   <li>FEFO (First-Expired-First-Out): lô hết hạn sớm nhất được lấy trước; lô không có hạn ưu tiên cuối.</li>
 *   <li>Ưu tiên vị trí gần cửa xuất kho (pathSequence nhỏ nhất).</li>
 * </ul>
 */
public interface PickingAllocationService {

    /**
     * Phân bổ vị trí lấy hàng cho 1 sản phẩm.
     *
     * @param productId       ID sản phẩm cần xuất
     * @param requiredQuantity Số lượng cần xuất
     * @param batchNo         Số lô cụ thể (nullable — nếu không null, chỉ lấy từ lô này)
     * @return Danh sách kết quả phân bổ, mỗi phần tử ứng với 1 PickingTask
     * @throws RuntimeException nếu tổng tồn kho khả dụng không đủ
     */
    List<AllocationResultDTO> allocate(Long productId, Integer requiredQuantity, String batchNo);
}

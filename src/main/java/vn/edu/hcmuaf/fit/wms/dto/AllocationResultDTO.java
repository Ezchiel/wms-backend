package vn.edu.hcmuaf.fit.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Kết quả phân bổ cho 1 vị trí / lô hàng cụ thể.
 * Được trả về bởi PickingAllocationService.allocate().
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class AllocationResultDTO {
    /** ID vị trí kho (storage_locations.id) */
    private Long locationId;

    /** Mã lô hàng cần lấy (nullable nếu không quản lý theo lô) */
    private String batchNo;

    /** Ngày hết hạn của lô, dùng để hiển thị cho nhân viên tham khảo */
    private LocalDate expiryDate;

    /** Số lượng cần lấy tại vị trí / lô này */
    private Integer quantity;
}

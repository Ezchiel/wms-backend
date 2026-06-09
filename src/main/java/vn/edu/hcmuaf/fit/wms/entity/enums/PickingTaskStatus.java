package vn.edu.hcmuaf.fit.wms.entity.enums;

public enum PickingTaskStatus {
    PENDING,     // Chờ lấy hàng
    IN_PROGRESS, // Đang lấy
    DONE,        // Đã lấy xong
    FAILED       // Không lấy được (thiếu hàng thực tế)
}

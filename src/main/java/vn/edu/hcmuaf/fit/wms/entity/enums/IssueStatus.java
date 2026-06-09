package vn.edu.hcmuaf.fit.wms.entity.enums;

public enum IssueStatus {
    DRAFT,      // Phiếu mới tạo, chờ duyệt
    APPROVED,   // Đã được quản lý duyệt
    PICKING,    // Đang trong quá trình lấy hàng
    COMPLETED,  // Đã xuất hàng, tồn kho đã trừ
    CANCELLED   // Đã huỷ
}

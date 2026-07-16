package vn.edu.hcmuaf.fit.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSummaryDTO {

    /** Loại cảnh báo: LOW_STOCK | EXPIRING_STOCK */
    private String type;

    /** Tên ngắn hiển thị (tên sản phẩm) */
    private String title;

    /** Mô tả chi tiết */
    private String message;

    /** Mức độ: CRITICAL | WARNING | INFO */
    private String severity;

    /** ID tham chiếu (productId với LOW_STOCK, stockId với EXPIRING_STOCK) */
    private Long referenceId;

    /** Loại tham chiếu: PRODUCT | STOCK */
    private String referenceType;

    /**
     * Số ngày còn lại đến hết hạn — chỉ có ý nghĩa với EXPIRING_STOCK,
     * null với LOW_STOCK.
     */
    private Integer daysRemaining;
}

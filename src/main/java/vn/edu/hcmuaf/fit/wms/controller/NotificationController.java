package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.ExpiringStockDTO;
import vn.edu.hcmuaf.fit.wms.dto.LowStockAlertDTO;
import vn.edu.hcmuaf.fit.wms.dto.NotificationSummaryDTO;
import vn.edu.hcmuaf.fit.wms.service.ReportService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Các API thông báo và cảnh báo hệ thống")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class NotificationController {

    private final ReportService reportService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách thông báo tổng hợp (cảnh báo tồn kho thấp và sắp hết hạn)",
            description = "Tổng hợp dữ liệu cảnh báo từ hệ thống, sắp xếp theo độ nghiêm trọng (CRITICAL > WARNING > INFO)."
    )
    public ResponseEntity<ApiResponse<List<NotificationSummaryDTO>>> getNotifications() {
        List<NotificationSummaryDTO> notifications = new ArrayList<>();

        // 1. Lấy cảnh báo tồn kho thấp
        List<LowStockAlertDTO> lowStockAlerts = reportService.getLowStockAlerts();
        if (lowStockAlerts != null) {
            for (LowStockAlertDTO alert : lowStockAlerts) {
                String severity;
                long total = alert.getCurrentTotalStock() != null ? alert.getCurrentTotalStock() : 0;
                int minLevel = alert.getMinStockLevel() != null ? alert.getMinStockLevel() : 0;

                if (total == 0) {
                    severity = "CRITICAL";
                } else if (total <= minLevel / 2) {
                    severity = "WARNING";
                } else {
                    severity = "INFO";
                }

                String message = String.format(
                        "Sản phẩm %s (%s) có mức tồn kho thấp: %d/%d",
                        alert.getProductName(),
                        alert.getProductCode(),
                        total,
                        minLevel
                );

                notifications.add(NotificationSummaryDTO.builder()
                        .type("LOW_STOCK")
                        .title("Cảnh báo tồn kho thấp")
                        .message(message)
                        .severity(severity)
                        .referenceId(alert.getProductId())
                        .referenceType("PRODUCT")
                        .daysRemaining(null)
                        .build());
            }
        }

        // 2. Lấy cảnh báo hàng sắp hết hạn (trong vòng 30 ngày)
        List<ExpiringStockDTO> expiringStock = reportService.getExpiringStock(30);
        if (expiringStock != null) {
            for (ExpiringStockDTO stock : expiringStock) {
                String severity;
                long days = stock.daysRemaining();

                if (days <= 3) {
                    severity = "CRITICAL";
                } else if (days <= 7) {
                    severity = "WARNING";
                } else {
                    severity = "INFO";
                }

                String message = String.format(
                        "Lô %s của sản phẩm %s tại vị trí %s sắp hết hạn (%d ngày còn lại)",
                        stock.batchNo() != null ? stock.batchNo() : "N/A",
                        stock.productName(),
                        stock.locationBarcode() != null ? stock.locationBarcode() : "N/A",
                        days
                );

                notifications.add(NotificationSummaryDTO.builder()
                        .type("EXPIRING_STOCK")
                        .title("Cảnh báo hàng sắp hết hạn")
                        .message(message)
                        .severity(severity)
                        .referenceId(stock.stockId())
                        .referenceType("STOCK")
                        .daysRemaining((int) days)
                        .build());
            }
        }

        // 3. Sắp xếp danh sách thông báo
        // Quy tắc: CRITICAL > WARNING > INFO
        // Nếu cùng độ nghiêm trọng: EXPIRING_STOCK đứng trước LOW_STOCK. Cùng EXPIRING_STOCK thì so sánh daysRemaining tăng dần.
        List<NotificationSummaryDTO> sortedNotifications = notifications.stream()
                .sorted(getNotificationComparator())
                .limit(20) // Giới hạn top 20 cảnh báo
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thông báo thành công",
                sortedNotifications
        ));
    }

    private Comparator<NotificationSummaryDTO> getNotificationComparator() {
        return (a, b) -> {
            int sevA = getSeverityValue(a.getSeverity());
            int sevB = getSeverityValue(b.getSeverity());
            if (sevA != sevB) {
                return Integer.compare(sevA, sevB);
            }

            // Cùng độ nghiêm trọng
            boolean isExpA = "EXPIRING_STOCK".equals(a.getType());
            boolean isExpB = "EXPIRING_STOCK".equals(b.getType());

            if (isExpA && isExpB) {
                Integer daysA = a.getDaysRemaining();
                Integer daysB = b.getDaysRemaining();
                if (daysA != null && daysB != null) {
                    return Integer.compare(daysA, daysB);
                }
                return 0;
            }

            if (isExpA) return -1; // EXPIRING_STOCK lên trước
            if (isExpB) return 1;  // LOW_STOCK xuống sau

            return 0;
        };
    }

    private int getSeverityValue(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) return 0;
        if ("WARNING".equalsIgnoreCase(severity)) return 1;
        return 2; // INFO
    }
}

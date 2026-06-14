package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.*;
import vn.edu.hcmuaf.fit.wms.service.ReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Các API báo cáo và thống kê tồn kho")
public class ReportController {

    private final ReportService reportService;

    /**
     * Báo cáo Nhập – Xuất – Tồn (NXT)
     */
    @GetMapping("/inventory-movement")
    @Operation(
        summary = "Báo cáo Nhập – Xuất – Tồn (NXT)",
        description = "Tổng hợp số lượng nhập, xuất, điều chỉnh và tồn cuối kỳ theo khoảng thời gian. " +
                      "Có thể lọc theo sản phẩm hoặc nhóm sản phẩm."
    )
    public ResponseEntity<ApiResponse<List<InventoryMovementDTO>>> getInventoryMovement(
            @Parameter(description = "Ngày bắt đầu kỳ (yyyy-MM-dd)", example = "2025-01-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Ngày kết thúc kỳ (yyyy-MM-dd)", example = "2025-01-31", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "ID sản phẩm (tuỳ chọn)")
            @RequestParam(required = false) Long productId,

            @Parameter(description = "ID nhóm sản phẩm (tuỳ chọn, bị bỏ qua nếu đã chọn productId)")
            @RequestParam(required = false) Long groupId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy báo cáo Nhập – Xuất – Tồn thành công",
                reportService.getInventoryMovementReport(from, to, productId, groupId)
        ));
    }

    /**
     * Xu hướng tồn kho theo thời gian
     */
    @GetMapping("/stock-trend")
    @Operation(
        summary = "Xu hướng tồn kho theo thời gian",
        description = "Dữ liệu biểu đồ đường thể hiện tồn kho theo từng ngày / tuần / tháng."
    )
    public ResponseEntity<ApiResponse<List<StockTrendDTO>>> getStockTrend(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", example = "2025-01-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", example = "2025-01-31", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Nhóm theo: day | week | month (mặc định: day)", example = "day")
            @RequestParam(defaultValue = "day") String groupBy,

            @Parameter(description = "ID sản phẩm (tuỳ chọn, null = tổng tất cả)")
            @RequestParam(required = false) Long productId
    ) {
        // Validate groupBy
        if (!groupBy.equalsIgnoreCase("day") &&
            !groupBy.equalsIgnoreCase("week") &&
            !groupBy.equalsIgnoreCase("month")) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Tham số groupBy không hợp lệ. Chỉ chấp nhận: day, week, month")
            );
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dữ liệu xu hướng tồn kho thành công",
                reportService.getStockTrend(from, to, groupBy, productId)
        ));
    }

    /**
     * Thống kê tồn kho theo nhóm sản phẩm
     */
    @GetMapping("/stock-by-group")
    @Operation(
        summary = "Thống kê tồn kho theo nhóm sản phẩm",
        description = "Tổng hợp tồn kho và giá trị ước tính theo từng nhóm sản phẩm (ProductGroup)."
    )
    public ResponseEntity<ApiResponse<List<StockByGroupDTO>>> getStockByGroup() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thống kê tồn kho theo nhóm sản phẩm thành công",
                reportService.getStockByGroup()
        ));
    }

    /**
     * Cảnh báo hàng sắp hết hạn
     */
    @GetMapping("/expiring-stock")
    @Operation(
        summary = "Cảnh báo hàng sắp hết hạn",
        description = "Liệt kê các lô hàng có ngày hết hạn (expiryDate) trong vòng N ngày tới."
    )
    public ResponseEntity<ApiResponse<List<ExpiringStockDTO>>> getExpiringStock(
            @Parameter(description = "Số ngày còn lại đến hết hạn (mặc định 30)", example = "30")
            @RequestParam(defaultValue = "30") int withinDays
    ) {
        if (withinDays < 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Tham số withinDays phải >= 0")
            );
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách hàng sắp hết hạn thành công",
                reportService.getExpiringStock(withinDays)
        ));
    }

    /**
     * Tỷ lệ sử dụng vị trí kho
     */
    @GetMapping("/location-utilization")
    @Operation(
        summary = "Thống kê tỷ lệ sử dụng vị trí kho",
        description = "Hiển thị bao nhiêu vị trí đang dùng, đang trống, tỷ lệ sử dụng (%) phân tách theo zone."
    )
    public ResponseEntity<ApiResponse<List<LocationUtilizationDTO>>> getLocationUtilization() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thống kê tỷ lệ sử dụng vị trí kho thành công",
                reportService.getLocationUtilization()
        ));
    }
}

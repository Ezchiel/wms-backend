package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.ChartDataDTO;
import vn.edu.hcmuaf.fit.wms.dto.LowStockAlertDTO;
import vn.edu.hcmuaf.fit.wms.service.ReportService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReportService reportService;

    @GetMapping("/summary/total-stock")
    public ResponseEntity<ApiResponse<Long>> getTotalStock() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy tổng số lượng tồn kho (tổng quan) thành công",
                reportService.getTotalInventoryStock()
        ));
    }

    @GetMapping("/charts/stock-by-product")
    public ResponseEntity<ApiResponse<List<ChartDataDTO>>> getStockByProduct() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dữ liệu biểu đồ tồn kho theo sản phẩm thành công",
                reportService.getStockByProductChart()
        ));
    }

    @GetMapping("/charts/stock-by-zone")
    public ResponseEntity<ApiResponse<List<ChartDataDTO>>> getStockByZone() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dữ liệu biểu đồ tồn kho theo dãy/khu vực thành công",
                reportService.getStockByZoneChart()
        ));
    }

    @GetMapping("/alerts/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockAlertDTO>>> getLowStockAlerts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách cảnh báo tồn kho thấp thành công",
                reportService.getLowStockAlerts()
        ));
    }
}

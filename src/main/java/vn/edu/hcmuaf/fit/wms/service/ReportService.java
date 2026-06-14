package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    Long getTotalInventoryStock();
    List<ChartDataDTO> getStockByProductChart();
    List<ChartDataDTO> getStockByZoneChart();
    List<LowStockAlertDTO> getLowStockAlerts();

    // Báo cáo Nhập – Xuất – Tồn (NXT)
    List<InventoryMovementDTO> getInventoryMovementReport(LocalDate from, LocalDate to, Long productId, Long groupId);

    // Cảnh báo hàng sắp hết hạn
    List<ExpiringStockDTO> getExpiringStock(int withinDays);

    // Thống kê theo nhóm sản phẩm
    List<StockByGroupDTO> getStockByGroup();

    // Tỷ lệ sử dụng vị trí kho
    List<LocationUtilizationDTO> getLocationUtilization();

    // Xu hướng tồn kho theo thời gian
    List<StockTrendDTO> getStockTrend(LocalDate from, LocalDate to, String groupBy, Long productId);
}

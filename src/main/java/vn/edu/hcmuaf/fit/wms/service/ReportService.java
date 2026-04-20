package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.ChartDataDTO;
import vn.edu.hcmuaf.fit.wms.dto.LowStockAlertDTO;

import java.util.List;

public interface ReportService {
    Long getTotalInventoryStock();
    List<ChartDataDTO> getStockByProductChart();
    List<ChartDataDTO> getStockByZoneChart();
    List<LowStockAlertDTO> getLowStockAlerts();
}

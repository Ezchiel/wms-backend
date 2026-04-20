package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.ChartDataDTO;
import vn.edu.hcmuaf.fit.wms.dto.LowStockAlertDTO;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.repository.InventoryStockRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.service.ReportService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final InventoryStockRepository stockRepository;
    private final ProductRepository productRepository;

    @Override
    public Long getTotalInventoryStock() {
        return stockRepository.getTotalStockQuantity();
    }

    @Override
    public List<ChartDataDTO> getStockByProductChart() {
        List<Object[]> results = stockRepository.countTotalStockByProduct();
        List<ChartDataDTO> chartData = new ArrayList<>();

        for (Object[] row : results) {
            Long productId = (Long) row[0];
            Long totalQuantity = (Long) row[1];

            String productName = productRepository.findById(productId)
                    .map(Product::getProductName)
                    .orElse("Sản phẩm không xác định");

            chartData.add(new ChartDataDTO(productName, totalQuantity));
        }

        return chartData;
    }

    @Override
    public List<ChartDataDTO> getStockByZoneChart() {
        List<Object[]> results = stockRepository.countTotalStockByZone();
        List<ChartDataDTO> chartData = new ArrayList<>();

        for (Object[] row : results) {
            String zoneName = (String) row[0];
            Long totalQuantity = (Long) row[1];

            chartData.add(new ChartDataDTO("Dãy " + zoneName, totalQuantity));
        }

        return chartData;
    }

    @Override
    public List<LowStockAlertDTO> getLowStockAlerts() {
        return productRepository.findProductsBelowMinStockLevel();
    }
}

package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.*;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.entity.InventoryTransaction;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.enums.TransactionType;
import vn.edu.hcmuaf.fit.wms.repository.*;
import vn.edu.hcmuaf.fit.wms.service.ReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final InventoryStockRepository stockRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StorageLocationRepository locationRepository;
    private final InventoryReceiptDetailRepository receiptDetailRepository;
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

    @Override
    public List<InventoryMovementDTO> getInventoryMovementReport(
            LocalDate from, LocalDate to, Long productId, Long groupId) {

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        // Lấy tồn hiện tại theo từng sản phẩm
        List<Object[]> currentStockRows = stockRepository.getCurrentStockPerProduct(productId);

        // Lấy các giao dịch trong kỳ (lọc theo product hoặc group)
        List<InventoryTransaction> transactions;
        if (productId != null) {
            transactions = transactionRepository.findByDateRangeAndProduct(fromDt, toDt, productId);
        } else if (groupId != null) {
            transactions = transactionRepository.findByDateRangeAndGroup(fromDt, toDt, groupId);
        } else {
            transactions = transactionRepository.findByDateRangeAndProduct(fromDt, toDt, null);
        }

        // Group transactions theo productId
        Map<Long, List<InventoryTransaction>> txByProduct = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getProduct().getId()));

        // Nếu lọc theo group, lọc currentStockRows phù hợp
        Set<Long> allowedProductIds = null;
        if (productId == null && groupId != null) {
            allowedProductIds = txByProduct.keySet();
        }

        List<InventoryMovementDTO> result = new ArrayList<>();

        for (Object[] row : currentStockRows) {
            Long pid = (Long) row[0];
            String pCode = (String) row[1];
            String pName = (String) row[2];
            long closingStock = ((Number) row[3]).longValue();

            // Bỏ qua sản phẩm không thuộc group lọc
            if (allowedProductIds != null && !allowedProductIds.contains(pid)) {
                continue;
            }

            List<InventoryTransaction> txList = txByProduct.getOrDefault(pid, Collections.emptyList());

            long totalReceipt = txList.stream()
                    .filter(t -> t.getTransactionType() == TransactionType.RECEIPT)
                    .mapToLong(InventoryTransaction::getQuantity)
                    .sum();

            long totalIssue = txList.stream()
                    .filter(t -> t.getTransactionType() == TransactionType.ISSUE)
                    .mapToLong(InventoryTransaction::getQuantity)
                    .sum();

            long totalAdjust = txList.stream()
                    .filter(t -> t.getTransactionType() == TransactionType.ADJUST)
                    .mapToLong(InventoryTransaction::getQuantity)
                    .sum();

            // openingStock = closingStock - (receipt - issue + adjust)
            long openingStock = closingStock - (totalReceipt - totalIssue + totalAdjust);

            result.add(new InventoryMovementDTO(
                    pid, pCode, pName,
                    openingStock, totalReceipt, totalIssue, totalAdjust, closingStock
            ));
        }

        return result;
    }

    @Override
    public List<ExpiringStockDTO> getExpiringStock(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(withinDays);

        List<InventoryStock> expiring = stockRepository.findExpiringSoon(cutoffDate);

        return expiring.stream().map(s -> new ExpiringStockDTO(
                s.getId(),
                s.getProduct().getId(),
                s.getProduct().getProductCode(),
                s.getProduct().getProductName(),
                s.getLocation().getId(),
                s.getLocation().getBarcode(),
                s.getBatchNo(),
                s.getExpiryDate(),
                ChronoUnit.DAYS.between(today, s.getExpiryDate()),
                s.getQuantity()
        )).collect(Collectors.toList());
    }

    @Override
    public List<StockByGroupDTO> getStockByGroup() {
        List<Object[]> rows = stockRepository.getStockByProductGroup();

        return rows.stream().map(row -> {
            Long gId = (Long) row[0];
            String gCode = (String) row[1];
            String gName = (String) row[2];
            Long totalQty = (Long) row[3];

            // totalValue = totalQuantity × giá nhập trung bình của nhóm
            BigDecimal avgPrice = receiptDetailRepository.findAvgUnitPriceByGroupId(gId);
            BigDecimal totalValue = (avgPrice != null && totalQty != null)
                    ? avgPrice.multiply(BigDecimal.valueOf(totalQty)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new StockByGroupDTO(gId, gCode, gName, totalQty, totalValue);
        }).collect(Collectors.toList());
    }

    @Override
    public List<LocationUtilizationDTO> getLocationUtilization() {
        List<Object[]> rows = locationRepository.getUtilizationByZone();

        return rows.stream().map(row -> {
            String zone = (String) row[0];
            int total = ((Number) row[1]).intValue();
            int full  = ((Number) row[2]).intValue();
            int empty = ((Number) row[3]).intValue();

            double rate = (total > 0)
                    ? Math.round((double) full / total * 10000.0) / 100.0
                    : 0.0;

            return new LocationUtilizationDTO(zone, total, full, empty, rate);
        }).collect(Collectors.toList());
    }

    @Override
    public List<StockTrendDTO> getStockTrend(LocalDate from, LocalDate to, String groupBy, Long productId) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);

        List<Object[]> rows;
        switch (groupBy.toLowerCase()) {
            case "week"  -> rows = transactionRepository.getStockTrendByWeek(fromDt, toDt, productId);
            case "month" -> rows = transactionRepository.getStockTrendByMonth(fromDt, toDt, productId);
            default      -> rows = transactionRepository.getStockTrendByDay(fromDt, toDt, productId);
        }

        // Tính tồn luỹ kế: mỗi period = tồn kỳ trước + net_change của kỳ này
        // Lấy tồn hiện tại làm điểm neo, tính ngược lại
        long currentStock = stockRepository.getTotalStockQuantity();

        // Tính tổng net_change từ cuối kỳ về trước để suy ra tồn từng kỳ
        long[] netChanges = new long[rows.size()];
        String[] periods  = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            periods[i]    = rows.get(i)[0].toString();
            netChanges[i] = ((Number) rows.get(i)[1]).longValue();
        }

        // Tồn cuối kỳ cuối = currentStock
        // Tồn cuối kỳ i = tồn cuối kỳ (i-1) + netChange[i]
        // → tính xuôi: khởi điểm tồn trước kỳ đầu tiên = currentStock - sum(netChanges)
        long totalNetInPeriod = Arrays.stream(netChanges).sum();
        long stockBeforePeriod = currentStock - totalNetInPeriod;

        List<StockTrendDTO> result = new ArrayList<>();
        long runningStock = stockBeforePeriod;
        for (int i = 0; i < rows.size(); i++) {
            runningStock += netChanges[i];
            result.add(new StockTrendDTO(periods[i], Math.max(0, runningStock)));
        }

        return result;
    }
}

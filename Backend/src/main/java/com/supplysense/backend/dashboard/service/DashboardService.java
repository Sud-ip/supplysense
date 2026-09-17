package com.supplysense.backend.dashboard.service;

import com.supplysense.backend.alerting.service.LowStockAlertService;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.dashboard.dto.DashboardSummaryResponse;
import com.supplysense.backend.dashboard.dto.StockTrendPointResponse;
import com.supplysense.backend.dashboard.dto.TopMoverResponse;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.domain.StockLedgerEntry;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.StockLedgerRepository;
import com.supplysense.backend.sales.repository.SaleRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Read-heavy aggregation queries across products, balances, and sales.
 * Kept as plain JPQL/derived queries and in-application grouping rather
 * than a separate reporting/analytics layer - at this product's target
 * scale (thousands of SKUs, not millions of rows), a dedicated reporting
 * layer would be complexity with no measured problem to justify it. If
 * dashboard load ever becomes a real bottleneck, that's a concrete signal
 * to revisit, not something to build in speculatively now.
 */
@Service
public class DashboardService {

    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int DEFAULT_TOP_MOVERS_DAYS = 30;
    private static final int DEFAULT_TOP_MOVERS_LIMIT = 10;

    private final ProductRepository productRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final SaleRepository saleRepository;
    private final LowStockAlertService lowStockAlertService;

    public DashboardService(
            ProductRepository productRepository,
            InventoryBalanceRepository inventoryBalanceRepository,
            StockLedgerRepository stockLedgerRepository,
            SaleRepository saleRepository,
            LowStockAlertService lowStockAlertService
    ) {
        this.productRepository = productRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.saleRepository = saleRepository;
        this.lowStockAlertService = lowStockAlertService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        UUID tenantId = TenantContext.currentTenantId();

        long totalActiveSkus = productRepository.countByTenantIdAndActiveTrue(tenantId);
        long lowStockCount = lowStockAlertService.getLowStockAlerts().size();

        BigDecimal totalInventoryValue = inventoryBalanceRepository.findAllByTenantId(tenantId).stream()
                .map(this::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardSummaryResponse(totalActiveSkus, lowStockCount, totalInventoryValue, Instant.now());
    }

    private BigDecimal valueOf(InventoryBalance balance) {
        BigDecimal unitCost = balance.getProduct().getUnitCost();
        if (unitCost == null) {
            return BigDecimal.ZERO;
        }
        return unitCost.multiply(BigDecimal.valueOf(balance.getQuantityOnHand()));
    }

    /**
     * Daily net stock movement over the window, grouped in application
     * code (not a DB-side date_trunc) - keeps this portable and avoids
     * Postgres-specific date functions for what's a small, bounded
     * dataset at this scale.
     */
    @Transactional(readOnly = true)
    public List<StockTrendPointResponse> getTrends(int days) {
        int windowDays = days > 0 ? days : DEFAULT_TREND_DAYS;
        UUID tenantId = TenantContext.currentTenantId();
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        List<StockLedgerEntry> entries = stockLedgerRepository.findAllByTenantIdAndCreatedAtAfter(tenantId, since);

        Map<LocalDate, int[]> byDate = new TreeMap<>(); // [0] = unitsIn, [1] = unitsOut
        for (StockLedgerEntry entry : entries) {
            LocalDate date = entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            int[] totals = byDate.computeIfAbsent(date, d -> new int[2]);
            if (entry.getQuantityDelta() > 0) {
                totals[0] += entry.getQuantityDelta();
            } else {
                totals[1] += -entry.getQuantityDelta();
            }
        }

        List<StockTrendPointResponse> trend = new ArrayList<>();
        for (var e : byDate.entrySet()) {
            int unitsIn = e.getValue()[0];
            int unitsOut = e.getValue()[1];
            trend.add(new StockTrendPointResponse(e.getKey(), unitsIn, unitsOut, unitsIn - unitsOut));
        }
        return trend;
    }

    @Transactional(readOnly = true)
    public List<TopMoverResponse> getTopMovers(int days, int limit) {
        int windowDays = days > 0 ? days : DEFAULT_TOP_MOVERS_DAYS;
        int resultLimit = limit > 0 ? limit : DEFAULT_TOP_MOVERS_LIMIT;
        UUID tenantId = TenantContext.currentTenantId();
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        var rows = saleRepository.findTopMoversSince(tenantId, since, PageRequest.of(0, resultLimit));

        List<TopMoverResponse> result = new ArrayList<>();
        for (var row : rows) {
            productRepository.findByIdAndTenantId(row.getProductId(), tenantId).ifPresent(product ->
                    result.add(new TopMoverResponse(product.getId(), product.getSku(), product.getName(), row.getTotalSold())));
        }
        return result;
    }
}

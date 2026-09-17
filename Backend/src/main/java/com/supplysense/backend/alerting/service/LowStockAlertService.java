package com.supplysense.backend.alerting.service;

import com.supplysense.backend.alerting.dto.AlertReason;
import com.supplysense.backend.alerting.dto.LowStockAlertResponse;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.forecasting.service.MovingAverageForecaster;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.domain.ReorderThreshold;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.ReorderThresholdRepository;
import com.supplysense.backend.sales.repository.SaleRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is where SupplySense's stated differentiation actually shows up in
 * code: PROJECTED_STOCKOUT_SOON can fire even when the current balance is
 * still comfortably above the manual reorder threshold, if the forecast
 * shows demand accelerating. A pure min/max system has no way to warn you
 * about that - it can only tell you once you've already crossed the
 * static line.
 *
 * Deliberately does NOT call ForecastingService.generateForecast() here -
 * that method persists a new forecast_results row every call, which is
 * fine for an explicit "show me forecasts" request but would be wasteful
 * if every low-stock-alerts check (potentially loaded on every dashboard
 * visit) wrote a history row for every thresholded product. So this
 * service computes the same moving-average math directly, read-only.
 */
@Service
public class LowStockAlertService {

    /** Kept in sync with ForecastingService.LOOKBACK_DAYS by convention - both represent "how far back is 'recent' sales history". */
    private static final int LOOKBACK_DAYS = 30;

    /** A stockout projected within this many days is worth surfacing now, even if the static threshold hasn't been crossed yet. */
    private static final int ALERT_WINDOW_DAYS = 7;

    private final ReorderThresholdRepository reorderThresholdRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final SaleRepository saleRepository;

    public LowStockAlertService(
            ReorderThresholdRepository reorderThresholdRepository,
            InventoryBalanceRepository inventoryBalanceRepository,
            SaleRepository saleRepository
    ) {
        this.reorderThresholdRepository = reorderThresholdRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> getLowStockAlerts() {
        UUID tenantId = TenantContext.currentTenantId();
        List<ReorderThreshold> thresholds = reorderThresholdRepository.findAllByTenantId(tenantId);

        List<LowStockAlertResponse> alerts = new ArrayList<>();
        for (ReorderThreshold threshold : thresholds) {
            if (!threshold.isActive()) {
                continue;
            }
            evaluateThreshold(tenantId, threshold).ifPresent(alerts::add);
        }
        return alerts;
    }

    private java.util.Optional<LowStockAlertResponse> evaluateThreshold(UUID tenantId, ReorderThreshold threshold) {
        Product product = threshold.getProduct();
        Location location = threshold.getLocation();

        int balance = inventoryBalanceRepository
                .findByTenantIdAndProductIdAndLocationId(tenantId, product.getId(), location.getId())
                .map(InventoryBalance::getQuantityOnHand)
                .orElse(0);

        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        int totalSold = saleRepository.sumQuantitySoldSince(tenantId, product.getId(), location.getId(), since);
        BigDecimal dailyDemand = MovingAverageForecaster.calculateDailyDemand(totalSold, LOOKBACK_DAYS);
        LocalDate stockoutDate = MovingAverageForecaster
                .projectStockoutDate(balance, dailyDemand, LocalDate.now())
                .orElse(null);

        List<AlertReason> reasons = new ArrayList<>();
        if (balance <= threshold.getMinQuantity()) {
            reasons.add(AlertReason.BELOW_MIN_THRESHOLD);
        }
        if (stockoutDate != null && !stockoutDate.isAfter(LocalDate.now().plusDays(ALERT_WINDOW_DAYS))) {
            reasons.add(AlertReason.PROJECTED_STOCKOUT_SOON);
        }

        if (reasons.isEmpty()) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new LowStockAlertResponse(
                product.getId(), product.getSku(), product.getName(),
                location.getId(), location.getName(),
                balance, threshold.getMinQuantity(), threshold.getReorderQuantity(),
                dailyDemand, stockoutDate, reasons));
    }
}

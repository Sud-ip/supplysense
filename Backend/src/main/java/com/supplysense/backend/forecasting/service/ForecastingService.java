package com.supplysense.backend.forecasting.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.forecasting.domain.ForecastMethod;
import com.supplysense.backend.forecasting.domain.ForecastResult;
import com.supplysense.backend.forecasting.dto.ForecastResponse;
import com.supplysense.backend.forecasting.repository.ForecastResultRepository;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.sales.repository.SaleRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ForecastingService {

    /** How far back we look to calculate average daily demand. */
    static final int LOOKBACK_DAYS = 30;

    /** Stored alongside each forecast as the horizon it's meaningful for. */
    static final int FORECAST_HORIZON_DAYS = 30;

    private final SaleRepository saleRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final TenantRepository tenantRepository;

    public ForecastingService(
            SaleRepository saleRepository,
            InventoryBalanceRepository inventoryBalanceRepository,
            ForecastResultRepository forecastResultRepository,
            TenantRepository tenantRepository
    ) {
        this.saleRepository = saleRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Generates (and persists, as a new history row) a forecast for one
     * product+location. Read endpoints call this directly rather than
     * relying on a scheduled job - fine at this product's target scale
     * (a handful of GET calls against a few thousand SKUs, not
     * high-frequency traffic). A scheduled/batch approach is a Phase 2
     * item if usage patterns ever justify it.
     */
    @Transactional
    public ForecastResponse generateForecast(UUID tenantId, Product product, Location location) {
        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        int totalSold = saleRepository.sumQuantitySoldSince(tenantId, product.getId(), location.getId(), since);

        BigDecimal dailyDemand = MovingAverageForecaster.calculateDailyDemand(totalSold, LOOKBACK_DAYS);

        int currentBalance = inventoryBalanceRepository
                .findByTenantIdAndProductIdAndLocationId(tenantId, product.getId(), location.getId())
                .map(InventoryBalance::getQuantityOnHand)
                .orElse(0);

        LocalDate stockoutDate = MovingAverageForecaster
                .projectStockoutDate(currentBalance, dailyDemand, LocalDate.now())
                .orElse(null);

        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        ForecastResult result = new ForecastResult(
                tenant, product, location, ForecastMethod.MOVING_AVERAGE,
                FORECAST_HORIZON_DAYS, dailyDemand, stockoutDate);

        return ForecastResponse.from(forecastResultRepository.save(result));
    }

    /**
     * Forecasts every product+location combination that currently has an
     * inventory balance row - i.e. has actually been stocked/transacted
     * at some point. This avoids generating meaningless forecasts for
     * every possible product x location pair a tenant could theoretically
     * have (most of which were never stocked together).
     */
    @Transactional
    public List<ForecastResponse> getForecasts(UUID productIdFilter) {
        UUID tenantId = TenantContext.currentTenantId();

        List<InventoryBalance> balances = productIdFilter != null
                ? inventoryBalanceRepository.findAllByTenantIdAndProductId(tenantId, productIdFilter)
                : inventoryBalanceRepository.findAllByTenantId(tenantId);

        return balances.stream()
                .map(balance -> generateForecast(tenantId, balance.getProduct(), balance.getLocation()))
                .toList();
    }
}

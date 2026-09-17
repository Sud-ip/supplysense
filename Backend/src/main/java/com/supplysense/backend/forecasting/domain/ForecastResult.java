package com.supplysense.backend.forecasting.domain;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Each generation creates a NEW row (append-only history), same pattern
 * as StockLedgerEntry/Sale - no setters, no update path. This is what
 * lets Phase 2 later compare "what did we predict on date X" against
 * "what actually happened" without needing to have planned for that
 * comparison in advance.
 */
@Entity
@Table(name = "forecast_results")
public class ForecastResult {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ForecastMethod method;

    @Column(name = "forecast_horizon_days", nullable = false)
    private int forecastHorizonDays;

    @Column(name = "predicted_daily_demand", nullable = false, precision = 12, scale = 4)
    private BigDecimal predictedDailyDemand;

    @Column(name = "projected_stockout_date")
    private LocalDate projectedStockoutDate;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    protected ForecastResult() {
        // JPA
    }

    public ForecastResult(
            Tenant tenant, Product product, Location location, ForecastMethod method,
            int forecastHorizonDays, BigDecimal predictedDailyDemand, LocalDate projectedStockoutDate
    ) {
        this.tenant = tenant;
        this.product = product;
        this.location = location;
        this.method = method;
        this.forecastHorizonDays = forecastHorizonDays;
        this.predictedDailyDemand = predictedDailyDemand;
        this.projectedStockoutDate = projectedStockoutDate;
    }

    @PrePersist
    void onCreate() {
        this.generatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Location getLocation() {
        return location;
    }

    public ForecastMethod getMethod() {
        return method;
    }

    public int getForecastHorizonDays() {
        return forecastHorizonDays;
    }

    public BigDecimal getPredictedDailyDemand() {
        return predictedDailyDemand;
    }

    public LocalDate getProjectedStockoutDate() {
        return projectedStockoutDate;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}

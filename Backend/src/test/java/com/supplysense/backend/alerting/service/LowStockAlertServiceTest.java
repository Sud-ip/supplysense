package com.supplysense.backend.alerting.service;

import com.supplysense.backend.alerting.dto.AlertReason;
import com.supplysense.backend.alerting.dto.LowStockAlertResponse;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.domain.ReorderThreshold;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.ReorderThresholdRepository;
import com.supplysense.backend.sales.repository.SaleRepository;
import com.supplysense.backend.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockAlertServiceTest {

    @Mock ReorderThresholdRepository reorderThresholdRepository;
    @Mock InventoryBalanceRepository inventoryBalanceRepository;
    @Mock SaleRepository saleRepository;

    @InjectMocks
    LowStockAlertService alertService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.AuthenticatedPrincipal(
                UUID.randomUUID(), tenantId, "owner@test.example", "OWNER"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void firesAlertWhenBalanceAtOrBelowMinThreshold() {
        Tenant tenant = new Tenant("Acme");
        Product product = new Product(tenant, "SKU-1", "Widget", null, BigDecimal.ONE, BigDecimal.TEN);
        Location location = new Location(tenant, "Main WH", null);
        ReorderThreshold threshold = new ReorderThreshold(tenant, product, location, 10, 50);

        // Build the balance mock and stub it FIRST, as its own complete
        // statement, before it's used as an argument anywhere else -
        // nesting a when()/thenReturn() pair inside another when()'s
        // argument list confuses Mockito's stubbing state even when each
        // one looks syntactically complete on its own.
        InventoryBalance balance = mock(InventoryBalance.class);
        when(balance.getQuantityOnHand()).thenReturn(5); // below the min of 10

        when(reorderThresholdRepository.findAllByTenantId(tenantId)).thenReturn(List.of(threshold));
        when(inventoryBalanceRepository.findByTenantIdAndProductIdAndLocationId(any(), any(), any()))
                .thenReturn(Optional.of(balance));
        when(saleRepository.sumQuantitySoldSince(any(), any(), any(), any())).thenReturn(0); // no recent sales

        List<LowStockAlertResponse> alerts = alertService.getLowStockAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).reasons()).contains(AlertReason.BELOW_MIN_THRESHOLD);
    }

    @Test
    void firesAlertWhenForecastProjectsStockoutSoonEvenAboveThreshold() {
        Tenant tenant = new Tenant("Acme");
        Product product = new Product(tenant, "SKU-2", "Fast Mover", null, BigDecimal.ONE, BigDecimal.TEN);
        Location location = new Location(tenant, "Main WH", null);
        ReorderThreshold threshold = new ReorderThreshold(tenant, product, location, 5, 50);

        InventoryBalance balance = mock(InventoryBalance.class);
        when(balance.getQuantityOnHand()).thenReturn(20);

        when(reorderThresholdRepository.findAllByTenantId(tenantId)).thenReturn(List.of(threshold));
        when(inventoryBalanceRepository.findByTenantIdAndProductIdAndLocationId(any(), any(), any()))
                .thenReturn(Optional.of(balance));
        when(saleRepository.sumQuantitySoldSince(any(), any(), any(), any())).thenReturn(300);

        List<LowStockAlertResponse> alerts = alertService.getLowStockAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).reasons()).contains(AlertReason.PROJECTED_STOCKOUT_SOON);
        assertThat(alerts.get(0).reasons()).doesNotContain(AlertReason.BELOW_MIN_THRESHOLD);
    }

    @Test
    void noAlertWhenBalanceHealthyAndNoStockoutProjected() {
        Tenant tenant = new Tenant("Acme");
        Product product = new Product(tenant, "SKU-3", "Slow Mover", null, BigDecimal.ONE, BigDecimal.TEN);
        Location location = new Location(tenant, "Main WH", null);
        ReorderThreshold threshold = new ReorderThreshold(tenant, product, location, 5, 50);

        InventoryBalance balance = mock(InventoryBalance.class);
        when(balance.getQuantityOnHand()).thenReturn(100);

        when(reorderThresholdRepository.findAllByTenantId(tenantId)).thenReturn(List.of(threshold));
        when(inventoryBalanceRepository.findByTenantIdAndProductIdAndLocationId(any(), any(), any()))
                .thenReturn(Optional.of(balance));
        when(saleRepository.sumQuantitySoldSince(any(), any(), any(), any())).thenReturn(30);

        List<LowStockAlertResponse> alerts = alertService.getLowStockAlerts();

        assertThat(alerts).isEmpty();
    }
}
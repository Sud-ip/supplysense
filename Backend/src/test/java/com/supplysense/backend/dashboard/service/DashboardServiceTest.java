package com.supplysense.backend.dashboard.service;

import com.supplysense.backend.alerting.dto.LowStockAlertResponse;
import com.supplysense.backend.alerting.service.LowStockAlertService;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.dashboard.dto.DashboardSummaryResponse;
import com.supplysense.backend.dashboard.dto.StockTrendPointResponse;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.domain.StockChangeReason;
import com.supplysense.backend.inventory.domain.StockLedgerEntry;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.StockLedgerRepository;
import com.supplysense.backend.sales.repository.SaleRepository;
import com.supplysense.backend.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ProductRepository productRepository;
    @Mock InventoryBalanceRepository inventoryBalanceRepository;
    @Mock StockLedgerRepository stockLedgerRepository;
    @Mock SaleRepository saleRepository;
    @Mock LowStockAlertService lowStockAlertService;

    @InjectMocks
    DashboardService dashboardService;

    private final UUID tenantId = UUID.randomUUID();
    private final Tenant tenant = new Tenant("Acme");

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
    void summaryTreatsNullUnitCostAsZeroValue() {
        Product withCost = new Product(tenant, "SKU-1", "Priced Widget", null, new BigDecimal("2.50"), BigDecimal.TEN);
        Product withoutCost = new Product(tenant, "SKU-2", "Unpriced Widget", null, null, BigDecimal.TEN);

        InventoryBalance balance1 = mock(InventoryBalance.class);
        when(balance1.getProduct()).thenReturn(withCost);
        when(balance1.getQuantityOnHand()).thenReturn(10); // 10 * 2.50 = 25.00

        InventoryBalance balance2 = mock(InventoryBalance.class);
        when(balance2.getProduct()).thenReturn(withoutCost);
        // No stub for balance2.getQuantityOnHand() - DashboardService.valueOf()
        // returns early for a null unitCost before ever reading quantity, so
        // stubbing it here would be genuinely unused (Mockito's strict
        // stubbing correctly flagged this).

        when(productRepository.countByTenantIdAndActiveTrue(tenantId)).thenReturn(2L);
        when(inventoryBalanceRepository.findAllByTenantId(tenantId)).thenReturn(List.of(balance1, balance2));
        when(lowStockAlertService.getLowStockAlerts()).thenReturn(List.<LowStockAlertResponse>of());

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.totalActiveSkus()).isEqualTo(2);
        assertThat(summary.lowStockCount()).isEqualTo(0);
        assertThat(summary.totalInventoryValue()).isEqualByComparingTo("25.00");
    }

    @Test
    void trendsGroupLedgerEntriesByCalendarDayAndSeparateInFromOut() throws Exception {
        User user = new User(tenant, "u@test.example", "hash", "User", com.supplysense.backend.auth.domain.Role.OWNER);
        Product product = new Product(tenant, "SKU-1", "Widget", null, BigDecimal.ONE, BigDecimal.TEN);
        Location location = new Location(tenant, "WH", null);

        Instant day1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(1, ChronoUnit.DAYS);

        StockLedgerEntry inEntry = buildLedgerEntry(tenant, product, location, 10, StockChangeReason.PURCHASE, user, day1);
        StockLedgerEntry outEntry = buildLedgerEntry(tenant, product, location, -3, StockChangeReason.SALE, user, day1);
        StockLedgerEntry secondDayEntry = buildLedgerEntry(tenant, product, location, 5, StockChangeReason.ADJUSTMENT, user, day2);

        when(stockLedgerRepository.findAllByTenantIdAndCreatedAtAfter(any(), any()))
                .thenReturn(List.of(inEntry, outEntry, secondDayEntry));

        List<StockTrendPointResponse> trend = dashboardService.getTrends(30);

        assertThat(trend).hasSize(2);
        // Sorted ascending by date (TreeMap) - day1's point comes first.
        StockTrendPointResponse firstDay = trend.get(0);
        assertThat(firstDay.unitsIn()).isEqualTo(10);
        assertThat(firstDay.unitsOut()).isEqualTo(3);
        assertThat(firstDay.netChange()).isEqualTo(7);

        StockTrendPointResponse secondDay = trend.get(1);
        assertThat(secondDay.unitsIn()).isEqualTo(5);
        assertThat(secondDay.unitsOut()).isEqualTo(0);
    }

    /**
     * StockLedgerEntry's createdAt is set via @PrePersist, which never
     * fires outside a real JPA persist - so for this pure unit test we
     * construct the entry normally, then use reflection to backdate
     * createdAt to a specific test instant. This keeps the test fast
     * (no database) while still exercising the real grouping logic
     * against realistic timestamps spanning multiple days.
     */
    private StockLedgerEntry buildLedgerEntry(
            Tenant tenant, Product product, Location location, int delta,
            StockChangeReason reason, User user, Instant createdAt
    ) throws Exception {
        Constructor<StockLedgerEntry> constructor = StockLedgerEntry.class.getDeclaredConstructor(
                Tenant.class, Product.class, Location.class, int.class,
                StockChangeReason.class, String.class, UUID.class, User.class);
        constructor.setAccessible(true);
        StockLedgerEntry entry = constructor.newInstance(tenant, product, location, delta, reason, "TEST", null, user);

        Field createdAtField = StockLedgerEntry.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(entry, createdAt);

        return entry;
    }
}

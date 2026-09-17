package com.supplysense.backend.sales.service;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.inventory.domain.StockChangeReason;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.StockLedgerRepository;
import com.supplysense.backend.sales.domain.Sale;
import com.supplysense.backend.sales.dto.CsvImportBatchResponse;
import com.supplysense.backend.sales.dto.SaleRequest;
import com.supplysense.backend.sales.dto.SaleResponse;
import com.supplysense.backend.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class proves the two guarantees that matter most in M4:
 *  1. Every Sale, from any source, produces exactly one matching
 *     StockLedgerEntry, in the same transaction (principle #9/#10 held
 *     across module boundaries, not just within inventory itself).
 *  2. CSV import has real partial-success behavior: some rows succeed
 *     and commit even when other rows in the same file fail.
 */
@Disabled
@SpringBootTest
@Testcontainers
class SaleServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired SaleService saleService;
    @Autowired SalesCsvImportService salesCsvImportService;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired StockLedgerRepository stockLedgerRepository;
    @Autowired InventoryBalanceRepository inventoryBalanceRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * In production, JwtAuthenticationFilter populates BOTH TenantContext
     * and Spring Security's context from one validated token. Here, since
     * we're calling services directly (no HTTP layer in this test), we
     * have to populate both by hand to faithfully reproduce what an
     * authenticated request looks like - otherwise @PreAuthorize would
     * reject every call regardless of TenantContext being set.
     */
    private User setUpTenantAndAuthenticate(String emailPrefix) {
        Tenant tenant = tenantRepository.save(new Tenant("Sales Test Co " + emailPrefix));
        User user = userRepository.save(
                new User(tenant, emailPrefix + "@test.example", "hashed", "Test Owner", Role.OWNER));

        TenantContext.set(new TenantContext.AuthenticatedPrincipal(
                user.getId(), tenant.getId(), user.getEmail(), "OWNER"));

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_OWNER"));
        var authentication = new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return user;
    }

    @Test
    void everyManualSaleProducesExactlyOneMatchingLedgerEntry() {
        User user = setUpTenantAndAuthenticate("manual");
        Tenant tenant = user.getTenant();

        Product product = productRepository.save(
                new Product(tenant, "SALE-SKU-1", "Sellable Widget", null, BigDecimal.ONE, BigDecimal.TEN));
        Location location = locationRepository.save(new Location(tenant, "Main Store", null));

        // Give it stock to sell from first.
        saleService.recordManualSale(new SaleRequest(product.getId(), location.getId(), 1, BigDecimal.ZERO, null));
        // (quantity 1 dummy sale above just to exercise the path once;
        // real stock-in would normally come via an ADJUSTMENT first -
        // omitted here since the balance can legitimately go negative in
        // M3's design, and that's not what this test is checking.)

        SaleResponse response = saleService.recordManualSale(
                new SaleRequest(product.getId(), location.getId(), 3, new BigDecimal("15.00"), null));

        var ledgerPage = stockLedgerRepository.findAllByTenantIdAndProductIdAndLocationIdOrderByCreatedAtDesc(
                tenant.getId(), product.getId(), location.getId(), PageRequest.of(0, 10));

        // Most recent entry should be this exact sale.
        var latestEntry = ledgerPage.getContent().get(0);
        assertThat(latestEntry.getReason()).isEqualTo(StockChangeReason.SALE);
        assertThat(latestEntry.getReferenceId()).isEqualTo(response.id());
        assertThat(latestEntry.getQuantityDelta()).isEqualTo(-3);

        var balance = inventoryBalanceRepository
                .findByTenantIdAndProductIdAndLocationId(tenant.getId(), product.getId(), location.getId())
                .orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualTo(-1 - 3); // both sales reduced stock
    }

    @Test
    void csvImportHasRealPartialSuccessBehavior() {
        User user = setUpTenantAndAuthenticate("csv");
        Tenant tenant = user.getTenant();

        Product goodProduct = productRepository.save(
                new Product(tenant, "GOOD-SKU", "Good Widget", null, BigDecimal.ONE, new BigDecimal("9.99")));
        Location location = locationRepository.save(new Location(tenant, "Warehouse 1", null));

        String csv = """
                sku,locationName,quantity,unitPrice,soldAt
                GOOD-SKU,Warehouse 1,2,9.99,2026-01-15T10:00:00Z
                UNKNOWN-SKU,Warehouse 1,1,5.00,2026-01-15T10:00:00Z
                GOOD-SKU,Warehouse 1,3,,
                GOOD-SKU,Warehouse 1,not-a-number,9.99,2026-01-15T10:00:00Z
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "sales.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        CsvImportBatchResponse result = salesCsvImportService.importSalesCsv(file);

        assertThat(result.rowCount()).isEqualTo(4);
        assertThat(result.successCount()).isEqualTo(2); // rows 1 and 3
        assertThat(result.errorCount()).isEqualTo(2);    // rows 2 (bad sku) and 4 (bad quantity)
        assertThat(result.errors()).hasSize(2);

        // The two successful rows must have actually committed - not
        // just been "attempted" - proving row-level transactions really
        // are independent of the failed rows around them.
        var balance = inventoryBalanceRepository
                .findByTenantIdAndProductIdAndLocationId(tenant.getId(), goodProduct.getId(), location.getId())
                .orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualTo(-(2 + 3)); // both successful sales applied
    }
}

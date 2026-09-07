package com.supplysense.backend.inventory.service;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.domain.StockChangeReason;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.StockLedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the test that proves the ledger/balance design actually holds
 * up under real concurrent writes - not just that the code compiles and
 * runs sequentially fine. Twenty threads hit the SAME product+location
 * simultaneously; if the atomic upsert (INSERT ... ON CONFLICT DO UPDATE)
 * has a race condition, this test fails intermittently. If a naive
 * read-then-write approach had been used instead, this test would fail
 * almost every run.
 */
@SpringBootTest
@Testcontainers
class InventoryServiceConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired InventoryService inventoryService;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired InventoryBalanceRepository inventoryBalanceRepository;
    @Autowired StockLedgerRepository stockLedgerRepository;

    @Test
    void concurrentStockChangesToSameProductLocationDoNotCorruptBalance() throws InterruptedException {
        Tenant tenant = tenantRepository.save(new Tenant("Concurrency Test Co"));
        User user = userRepository.save(
                new User(tenant, "concurrency@test.example", "hashed", "Test User", Role.OWNER));
        Product product = productRepository.save(
                new Product(tenant, "CONC-1", "Concurrent Widget", null, BigDecimal.ONE, BigDecimal.TEN));
        Location location = locationRepository.save(new Location(tenant, "Main Warehouse", null));

        int threadCount = 20;
        int deltaPerThread = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    inventoryService.recordStockChange(
                            tenant.getId(), product, location, deltaPerThread,
                            StockChangeReason.ADJUSTMENT, "CONCURRENCY_TEST", null, user);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown(); // release all threads at once to maximize contention
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("all threads completed within timeout").isTrue();

        InventoryBalance balance = inventoryBalanceRepository
                .findByTenantIdAndProductIdAndLocationId(tenant.getId(), product.getId(), location.getId())
                .orElseThrow();

        assertThat(balance.getQuantityOnHand()).isEqualTo(threadCount * deltaPerThread);

        // Every concurrent call must have recorded its own immutable fact -
        // proves nothing was silently lost or merged, only the balance
        // aggregation was safely concurrent.
        long ledgerEntryCount = stockLedgerRepository
                .findAllByTenantIdOrderByCreatedAtDesc(tenant.getId(), PageRequest.of(0, 1000))
                .getTotalElements();

        assertThat(ledgerEntryCount).isEqualTo(threadCount);
    }
}

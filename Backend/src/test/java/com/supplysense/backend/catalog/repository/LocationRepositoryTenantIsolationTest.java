package com.supplysense.backend.catalog.repository;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.domain.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the single most important test in the catalog module: it proves
 * that TenantScopedRepository's tenant-filtered queries actually isolate
 * data between tenants. If this test ever fails, nothing built on top of
 * the catalog module (inventory, sales, forecasting) can be trusted.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class LocationRepositoryTenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    TenantRepository tenantRepository;

    @Test
    void locationsAreCompletelyIsolatedBetweenTenants() {
        Tenant tenantA = tenantRepository.save(new Tenant("Tenant A Retail"));
        Tenant tenantB = tenantRepository.save(new Tenant("Tenant B Retail"));

        locationRepository.save(new Location(tenantA, "Warehouse A1", "1 A St"));
        locationRepository.save(new Location(tenantA, "Warehouse A2", "2 A St"));
        locationRepository.save(new Location(tenantB, "Warehouse B1", "1 B St"));

        List<Location> tenantALocations = locationRepository.findAllByTenantId(tenantA.getId());
        List<Location> tenantBLocations = locationRepository.findAllByTenantId(tenantB.getId());

        assertThat(tenantALocations).hasSize(2);
        assertThat(tenantALocations).allMatch(loc -> loc.getTenant().getId().equals(tenantA.getId()));

        assertThat(tenantBLocations).hasSize(1);
        assertThat(tenantBLocations).allMatch(loc -> loc.getTenant().getId().equals(tenantB.getId()));
    }

    @Test
    void findByIdAndTenantIdReturnsEmptyForWrongTenant() {
        Tenant tenantA = tenantRepository.save(new Tenant("Tenant A"));
        Tenant tenantB = tenantRepository.save(new Tenant("Tenant B"));

        Location locationInA = locationRepository.save(new Location(tenantA, "A Warehouse", null));

        // Requesting tenant A's location while "logged in" as tenant B
        // must return empty - this is the exact query pattern every
        // catalog/inventory service method uses.
        assertThat(locationRepository.findByIdAndTenantId(locationInA.getId(), tenantB.getId()))
                .isEmpty();

        assertThat(locationRepository.findByIdAndTenantId(locationInA.getId(), tenantA.getId()))
                .isPresent();
    }
}

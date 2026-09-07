package com.supplysense.backend.inventory.repository;

import com.supplysense.backend.catalog.repository.TenantScopedRepository;
import com.supplysense.backend.inventory.domain.ReorderThreshold;

import java.util.Optional;
import java.util.UUID;

// Reuses the catalog module's TenantScopedRepository base - reorder
// thresholds are legitimately mutable (unlike the ledger), so the normal
// tenant-scoped CRUD pattern from M2 applies as-is.
public interface ReorderThresholdRepository extends TenantScopedRepository<ReorderThreshold, UUID> {

    Optional<ReorderThreshold> findByTenantIdAndProductIdAndLocationId(
            UUID tenantId, UUID productId, UUID locationId);
}

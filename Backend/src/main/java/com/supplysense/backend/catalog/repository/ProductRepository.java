package com.supplysense.backend.catalog.repository;

import com.supplysense.backend.catalog.domain.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends TenantScopedRepository<Product, UUID> {

    boolean existsBySkuAndTenantId(String sku, UUID tenantId);

    Optional<Product> findBySkuAndTenantId(String sku, UUID tenantId);

    // Added in M6: dashboard summary needs a count, not the full entity list.
    long countByTenantIdAndActiveTrue(UUID tenantId);
}

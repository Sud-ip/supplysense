package com.supplysense.backend.catalog.repository;

import com.supplysense.backend.catalog.domain.Product;

import java.util.UUID;

public interface ProductRepository extends TenantScopedRepository<Product, UUID> {

    boolean existsBySkuAndTenantId(String sku, UUID tenantId);
}

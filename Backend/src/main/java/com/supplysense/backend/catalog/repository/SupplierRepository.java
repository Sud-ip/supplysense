package com.supplysense.backend.catalog.repository;

import com.supplysense.backend.catalog.domain.Supplier;

import java.util.UUID;

public interface SupplierRepository extends TenantScopedRepository<Supplier, UUID> {
}

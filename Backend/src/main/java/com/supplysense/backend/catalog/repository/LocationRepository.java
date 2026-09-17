package com.supplysense.backend.catalog.repository;

import com.supplysense.backend.catalog.domain.Location;

import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends TenantScopedRepository<Location, UUID> {

    // Added in M4: CSV import resolves locations by name (the fixed
    // template's "locationName" column), not by UUID.
    Optional<Location> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}

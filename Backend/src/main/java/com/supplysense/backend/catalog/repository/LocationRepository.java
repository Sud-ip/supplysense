package com.supplysense.backend.catalog.repository;

import com.supplysense.backend.catalog.domain.Location;

import java.util.UUID;

public interface LocationRepository extends TenantScopedRepository<Location, UUID> {
}

package com.supplysense.backend.catalog.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.dto.LocationRequest;
import com.supplysense.backend.catalog.dto.LocationResponse;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.security.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final TenantRepository tenantRepository;

    public LocationService(LocationRepository locationRepository, TenantRepository tenantRepository) {
        this.locationRepository = locationRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<LocationResponse> findAll() {
        UUID tenantId = TenantContext.currentTenantId();
        return locationRepository.findAllByTenantId(tenantId).stream()
                .map(LocationResponse::from)
                .toList();
    }

    public LocationResponse findById(UUID id) {
        Location location = getOwnedOrThrow(id);
        return LocationResponse.from(location);
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public LocationResponse create(LocationRequest request) {
        UUID tenantId = TenantContext.currentTenantId();
        // Tenant is always looked up by the ID from the authenticated
        // JWT (TenantContext), never trusted from client input.
        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        Location location = new Location(tenant, request.name(), request.address());
        return LocationResponse.from(locationRepository.save(location));
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = getOwnedOrThrow(id);
        location.setName(request.name());
        location.setAddress(request.address());
        return LocationResponse.from(location);
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public void deactivate(UUID id) {
        Location location = getOwnedOrThrow(id);
        location.deactivate();
    }

    private Location getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.currentTenantId();
        return locationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }
}

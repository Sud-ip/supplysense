package com.supplysense.backend.inventory.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.inventory.domain.ReorderThreshold;
import com.supplysense.backend.inventory.dto.ReorderThresholdRequest;
import com.supplysense.backend.inventory.dto.ReorderThresholdResponse;
import com.supplysense.backend.inventory.repository.ReorderThresholdRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReorderThresholdService {

    private final ReorderThresholdRepository thresholdRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final TenantRepository tenantRepository;

    public ReorderThresholdService(
            ReorderThresholdRepository thresholdRepository,
            ProductRepository productRepository,
            LocationRepository locationRepository,
            TenantRepository tenantRepository
    ) {
        this.thresholdRepository = thresholdRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<ReorderThresholdResponse> findAll() {
        UUID tenantId = TenantContext.currentTenantId();
        return thresholdRepository.findAllByTenantId(tenantId).stream()
                .map(ReorderThresholdResponse::from)
                .toList();
    }

    /**
     * Upsert semantics: setting a threshold for a product+location that
     * already has one updates it, rather than erroring with a duplicate
     * conflict - this matches how a store owner actually thinks about it
     * ("set the reorder point for this product here to 10"), not as a
     * create-vs-update distinction they need to know about.
     */
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public ReorderThresholdResponse upsert(ReorderThresholdRequest request) {
        UUID tenantId = TenantContext.currentTenantId();

        Product product = productRepository.findByIdAndTenantId(request.productId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        Location location = locationRepository.findByIdAndTenantId(request.locationId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", request.locationId()));

        ReorderThreshold threshold = thresholdRepository
                .findByTenantIdAndProductIdAndLocationId(tenantId, request.productId(), request.locationId())
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.getReferenceById(tenantId);
                    return new ReorderThreshold(tenant, product, location, 0, 0);
                });

        threshold.setMinQuantity(request.minQuantity());
        threshold.setReorderQuantity(request.reorderQuantity());

        if (threshold.getId() == null) {
            threshold = thresholdRepository.save(threshold);
        }

        return ReorderThresholdResponse.from(threshold);
    }
}

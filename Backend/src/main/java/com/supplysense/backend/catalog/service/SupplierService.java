package com.supplysense.backend.catalog.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.domain.Supplier;
import com.supplysense.backend.catalog.dto.SupplierRequest;
import com.supplysense.backend.catalog.dto.SupplierResponse;
import com.supplysense.backend.catalog.repository.SupplierRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.security.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;

    public SupplierService(SupplierRepository supplierRepository, TenantRepository tenantRepository) {
        this.supplierRepository = supplierRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<SupplierResponse> findAll() {
        UUID tenantId = TenantContext.currentTenantId();
        return supplierRepository.findAllByTenantId(tenantId).stream()
                .map(SupplierResponse::from)
                .toList();
    }

    public SupplierResponse findById(UUID id) {
        return SupplierResponse.from(getOwnedOrThrow(id));
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        UUID tenantId = TenantContext.currentTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        Supplier supplier = new Supplier(tenant, request.name(), request.contactInfo());
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request) {
        Supplier supplier = getOwnedOrThrow(id);
        supplier.setName(request.name());
        supplier.setContactInfo(request.contactInfo());
        return SupplierResponse.from(supplier);
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public void deactivate(UUID id) {
        getOwnedOrThrow(id).deactivate();
    }

    private Supplier getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.currentTenantId();
        return supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }
}

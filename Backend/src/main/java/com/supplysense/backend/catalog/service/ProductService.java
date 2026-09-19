package com.supplysense.backend.catalog.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.domain.Supplier;
import com.supplysense.backend.catalog.dto.ProductRequest;
import com.supplysense.backend.catalog.dto.ProductResponse;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.catalog.repository.SupplierRepository;
import com.supplysense.backend.common.exception.DuplicateSkuException;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.security.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;

    public ProductService(
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            TenantRepository tenantRepository
    ) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        UUID tenantId = TenantContext.currentTenantId();
        boolean includeCost = canSeeCost();
        return productRepository.findAllByTenantId(tenantId).stream()
                .map(p -> ProductResponse.from(p, includeCost))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {

        return ProductResponse.from(getOwnedOrThrow(id), canSeeCost());
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public ProductResponse create(ProductRequest request) {
        UUID tenantId = TenantContext.currentTenantId();

        if (productRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
            throw new DuplicateSkuException(request.sku());
        }

        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Supplier supplier = resolveSupplierOrNull(request.supplierId(), tenantId);

        Product product = new Product(
                tenant, request.sku(), request.name(), supplier,
                request.unitCost(), request.unitPrice());

        return ProductResponse.from(productRepository.save(product), true);
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getOwnedOrThrow(id);
        UUID tenantId = TenantContext.currentTenantId();

        // SKU change must still respect uniqueness, excluding this product itself.
        if (!product.getSku().equals(request.sku())
                && productRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
            throw new DuplicateSkuException(request.sku());
        }

        Supplier supplier = resolveSupplierOrNull(request.supplierId(), tenantId);

        product.setName(request.name());
        product.setSupplier(supplier);
        product.setUnitCost(request.unitCost());
        product.setUnitPrice(request.unitPrice());

        return ProductResponse.from(product, true);
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public void deactivate(UUID id) {
        getOwnedOrThrow(id).deactivate();
    }

    private Product getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.currentTenantId();
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    /**
     * Looking up the supplier via findByIdAndTenantId (not just findById)
     * means a supplier belonging to a different tenant can never be
     * attached to this product - the tenant check is structural, not a
     * separate "if supplier.tenant != tenant" guard that could be
     * forgotten.
     */
    private Supplier resolveSupplierOrNull(UUID supplierId, UUID tenantId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findByIdAndTenantId(supplierId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
    }

    private boolean canSeeCost() {
        // ASSUMPTION (flagged in ProductResponse javadoc): STAFF does not
        // see unitCost. Change this predicate if that assumption is wrong.
        String role = TenantContext.get().role();
        return "OWNER".equals(role) || "MANAGER".equals(role);
    }
}

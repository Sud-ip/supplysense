package com.supplysense.backend.catalog.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.catalog.dto.ProductRequest;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.catalog.repository.SupplierRepository;
import com.supplysense.backend.common.exception.DuplicateSkuException;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock TenantRepository tenantRepository;

    @InjectMocks
    ProductService productService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.AuthenticatedPrincipal(
                UUID.randomUUID(), tenantId, "owner@acme.test", "OWNER"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRejectsDuplicateSkuWithinTenant() {
        when(productRepository.existsBySkuAndTenantId("SKU-1", tenantId)).thenReturn(true);

        ProductRequest request = new ProductRequest("SKU-1", "Widget", null,
                BigDecimal.TEN, BigDecimal.valueOf(19.99));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void createRejectsSupplierFromAnotherTenant() {
        UUID foreignSupplierId = UUID.randomUUID();

        when(productRepository.existsBySkuAndTenantId(any(), any())).thenReturn(false);
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(new Tenant("Acme"));
        // findByIdAndTenantId with THIS tenant's id returns empty, because
        // the supplier actually belongs to a different tenant - exactly
        // what would happen with a real cross-tenant supplierId.
        when(supplierRepository.findByIdAndTenantId(eq(foreignSupplierId), eq(tenantId)))
                .thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest("SKU-2", "Gadget", foreignSupplierId,
                BigDecimal.ONE, BigDecimal.TEN);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package com.supplysense.backend.catalog.dto;

import com.supplysense.backend.catalog.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * unitCost is nullable in the OUTPUT, not just the schema: ProductService
 * omits it for STAFF-role callers (margin-sensitive data). This is an
 * ASSUMPTION pending your confirmation from the open question raised in
 * the architecture doc - easy to flip in ProductService.toResponse() if
 * you'd rather STAFF see cost too.
 */
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        UUID supplierId,
        String supplierName,
        BigDecimal unitCost,
        BigDecimal unitPrice,
        boolean active,
        Instant createdAt
) {
    public static ProductResponse from(Product product, boolean includeCost) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                product.getSupplier() != null ? product.getSupplier().getName() : null,
                includeCost ? product.getUnitCost() : null,
                product.getUnitPrice(),
                product.isActive(),
                product.getCreatedAt()
        );
    }
}

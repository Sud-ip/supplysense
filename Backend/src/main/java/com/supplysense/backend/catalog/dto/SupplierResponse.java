package com.supplysense.backend.catalog.dto;

import com.supplysense.backend.catalog.domain.Supplier;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String contactInfo,
        boolean active,
        Instant createdAt
) {
    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(), supplier.getName(), supplier.getContactInfo(),
                supplier.isActive(), supplier.getCreatedAt());
    }
}

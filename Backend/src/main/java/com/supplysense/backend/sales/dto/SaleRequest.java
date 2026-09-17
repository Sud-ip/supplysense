package com.supplysense.backend.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleRequest(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "locationId is required")
        UUID locationId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be positive")
        Integer quantity,

        // Nullable on purpose: defaults to the product's current
        // unitPrice if omitted (the sale price can differ from the
        // product's listed price, e.g. a discount, so this is
        // deliberately overridable, not required).
        @DecimalMin(value = "0.0", message = "unitPrice must not be negative")
        BigDecimal unitPrice,

        // Nullable: defaults to "now" if omitted.
        Instant soldAt
) {
}

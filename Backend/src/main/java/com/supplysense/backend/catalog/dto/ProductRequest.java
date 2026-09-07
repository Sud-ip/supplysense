package com.supplysense.backend.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "sku is required")
        String sku,

        @NotBlank(message = "name is required")
        String name,

        UUID supplierId,

        @DecimalMin(value = "0.0", inclusive = true, message = "unitCost must not be negative")
        BigDecimal unitCost,

        @DecimalMin(value = "0.0", inclusive = true, message = "unitPrice must not be negative")
        BigDecimal unitPrice
) {
}

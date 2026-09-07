package com.supplysense.backend.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReorderThresholdRequest(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "locationId is required")
        UUID locationId,

        @NotNull(message = "minQuantity is required")
        @Min(value = 0, message = "minQuantity must not be negative")
        Integer minQuantity,

        @NotNull(message = "reorderQuantity is required")
        @Min(value = 0, message = "reorderQuantity must not be negative")
        Integer reorderQuantity
) {
}

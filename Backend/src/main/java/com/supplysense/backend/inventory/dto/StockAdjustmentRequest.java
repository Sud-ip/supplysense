package com.supplysense.backend.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "locationId is required")
        UUID locationId,

        @NotNull(message = "quantityDelta is required")
        Integer quantityDelta,

        @NotNull(message = "reason is required")
        AdjustmentReason reason,

        String note
) {
}

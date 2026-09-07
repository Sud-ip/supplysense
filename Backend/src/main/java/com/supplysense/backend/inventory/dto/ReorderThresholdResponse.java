package com.supplysense.backend.inventory.dto;

import com.supplysense.backend.inventory.domain.ReorderThreshold;

import java.time.Instant;
import java.util.UUID;

public record ReorderThresholdResponse(
        UUID id,
        UUID productId,
        String productSku,
        UUID locationId,
        String locationName,
        int minQuantity,
        int reorderQuantity,
        Instant updatedAt
) {
    public static ReorderThresholdResponse from(ReorderThreshold threshold) {
        return new ReorderThresholdResponse(
                threshold.getId(),
                threshold.getProduct().getId(),
                threshold.getProduct().getSku(),
                threshold.getLocation().getId(),
                threshold.getLocation().getName(),
                threshold.getMinQuantity(),
                threshold.getReorderQuantity(),
                threshold.getCreatedAt()
        );
    }
}

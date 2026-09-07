package com.supplysense.backend.inventory.dto;

import com.supplysense.backend.inventory.domain.InventoryBalance;

import java.time.Instant;
import java.util.UUID;

public record InventoryBalanceResponse(
        UUID productId,
        String productSku,
        String productName,
        UUID locationId,
        String locationName,
        int quantityOnHand,
        Instant updatedAt
) {
    public static InventoryBalanceResponse from(InventoryBalance balance) {
        return new InventoryBalanceResponse(
                balance.getProduct().getId(),
                balance.getProduct().getSku(),
                balance.getProduct().getName(),
                balance.getLocation().getId(),
                balance.getLocation().getName(),
                balance.getQuantityOnHand(),
                balance.getUpdatedAt()
        );
    }
}

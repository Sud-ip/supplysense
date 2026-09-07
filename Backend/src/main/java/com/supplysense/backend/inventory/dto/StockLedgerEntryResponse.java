package com.supplysense.backend.inventory.dto;

import com.supplysense.backend.inventory.domain.StockChangeReason;
import com.supplysense.backend.inventory.domain.StockLedgerEntry;

import java.time.Instant;
import java.util.UUID;

public record StockLedgerEntryResponse(
        UUID id,
        UUID productId,
        String productSku,
        UUID locationId,
        String locationName,
        int quantityDelta,
        StockChangeReason reason,
        String referenceType,
        UUID referenceId,
        UUID createdByUserId,
        String createdByEmail,
        Instant createdAt
) {
    public static StockLedgerEntryResponse from(StockLedgerEntry entry) {
        return new StockLedgerEntryResponse(
                entry.getId(),
                entry.getProduct().getId(),
                entry.getProduct().getSku(),
                entry.getLocation().getId(),
                entry.getLocation().getName(),
                entry.getQuantityDelta(),
                entry.getReason(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getCreatedBy().getId(),
                entry.getCreatedBy().getEmail(),
                entry.getCreatedAt()
        );
    }
}

package com.supplysense.backend.dashboard.dto;

import java.util.UUID;

public record TopMoverResponse(
        UUID productId,
        String productSku,
        String productName,
        long totalUnitsSold
) {
}

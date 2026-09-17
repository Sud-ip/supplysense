package com.supplysense.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DashboardSummaryResponse(
        long totalActiveSkus,
        long lowStockCount,
        BigDecimal totalInventoryValue,
        Instant generatedAt
) {
}

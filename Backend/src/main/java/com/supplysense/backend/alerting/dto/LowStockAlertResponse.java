package com.supplysense.backend.alerting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LowStockAlertResponse(
        UUID productId,
        String productSku,
        String productName,
        UUID locationId,
        String locationName,
        int quantityOnHand,
        int minQuantity,
        int reorderQuantity,
        BigDecimal predictedDailyDemand,
        LocalDate projectedStockoutDate,
        List<AlertReason> reasons
) {
}

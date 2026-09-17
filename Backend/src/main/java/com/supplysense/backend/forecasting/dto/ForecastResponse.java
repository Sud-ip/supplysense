package com.supplysense.backend.forecasting.dto;

import com.supplysense.backend.forecasting.domain.ForecastMethod;
import com.supplysense.backend.forecasting.domain.ForecastResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ForecastResponse(
        UUID productId,
        String productSku,
        UUID locationId,
        String locationName,
        ForecastMethod method,
        int forecastHorizonDays,
        BigDecimal predictedDailyDemand,
        LocalDate projectedStockoutDate,
        Instant generatedAt
) {
    public static ForecastResponse from(ForecastResult result) {
        return new ForecastResponse(
                result.getProduct().getId(),
                result.getProduct().getSku(),
                result.getLocation().getId(),
                result.getLocation().getName(),
                result.getMethod(),
                result.getForecastHorizonDays(),
                result.getPredictedDailyDemand(),
                result.getProjectedStockoutDate(),
                result.getGeneratedAt()
        );
    }
}

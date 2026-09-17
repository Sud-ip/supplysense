package com.supplysense.backend.dashboard.dto;

import java.time.LocalDate;

public record StockTrendPointResponse(
        LocalDate date,
        int unitsIn,
        int unitsOut,
        int netChange
) {
}

package com.supplysense.backend.forecasting.repository;

import com.supplysense.backend.forecasting.domain.ForecastResult;
import org.springframework.data.repository.Repository;

import java.util.UUID;

// Append-only, same pattern as StockLedgerRepository/SaleRepository -
// only save(), no update/delete exposed.
public interface ForecastResultRepository extends Repository<ForecastResult, UUID> {

    ForecastResult save(ForecastResult result);
}

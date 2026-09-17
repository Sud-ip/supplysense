-- V6__forecasting_schema.sql
-- Forecast results are stored as an append-only history (one row per
-- generation), not upserted - this gives us, for free, the historical
-- snapshots Phase 2's "model-vs-baseline evaluation" will need to compare
-- past predictions against what actually happened.

CREATE TABLE forecast_results (
    id                        UUID PRIMARY KEY,
    tenant_id                 UUID NOT NULL REFERENCES tenants(id),
    product_id                UUID NOT NULL REFERENCES products(id),
    location_id               UUID NOT NULL REFERENCES locations(id),
    method                    VARCHAR(30) NOT NULL
                                  CHECK (method IN ('MOVING_AVERAGE','EXPONENTIAL_SMOOTHING')),
    forecast_horizon_days     INTEGER NOT NULL,
    predicted_daily_demand    NUMERIC(12, 4) NOT NULL,
    projected_stockout_date   DATE,
    generated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_forecast_tenant_product_location ON forecast_results (tenant_id, product_id, location_id);
CREATE INDEX idx_forecast_generated_at ON forecast_results (generated_at);

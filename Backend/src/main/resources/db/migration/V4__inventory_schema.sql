-- V4__inventory_schema.sql
-- The core of SupplySense: an append-only stock ledger, a derived balance
-- projection, and per-product/location reorder thresholds.

CREATE TABLE stock_ledger_entries (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    product_id      UUID NOT NULL REFERENCES products(id),
    location_id     UUID NOT NULL REFERENCES locations(id),
    quantity_delta  INTEGER NOT NULL CHECK (quantity_delta <> 0),
    reason          VARCHAR(20) NOT NULL
                        CHECK (reason IN ('PURCHASE','SALE','ADJUSTMENT','RETURN','TRANSFER')),
    reference_type  VARCHAR(50),
    reference_id    UUID,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()

    -- Deliberately NO updated_at, NO soft-delete flag. This table is
    -- append-only by design (principle #9) - enforced at the application
    -- layer (StockLedgerRepository exposes no update/delete methods, and
    -- StockLedgerEntry has no setters at all).
);
CREATE INDEX idx_ledger_tenant_id ON stock_ledger_entries (tenant_id);
CREATE INDEX idx_ledger_product_location ON stock_ledger_entries (product_id, location_id);
CREATE INDEX idx_ledger_created_at ON stock_ledger_entries (created_at);

CREATE TABLE inventory_balances (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL REFERENCES tenants(id),
    product_id         UUID NOT NULL REFERENCES products(id),
    location_id        UUID NOT NULL REFERENCES locations(id),
    quantity_on_hand   INTEGER NOT NULL DEFAULT 0,
    last_ledger_id     UUID,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_balance_tenant_product_location UNIQUE (tenant_id, product_id, location_id)

    -- This table is a PROJECTION of stock_ledger_entries. It is only ever
    -- written via an atomic INSERT ... ON CONFLICT DO UPDATE in the same
    -- transaction as a ledger insert (principle #10) - never edited
    -- directly by any other code path.
);
CREATE INDEX idx_balance_tenant_id ON inventory_balances (tenant_id);

CREATE TABLE reorder_thresholds (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    product_id        UUID NOT NULL REFERENCES products(id),
    location_id       UUID NOT NULL REFERENCES locations(id),
    min_quantity      INTEGER NOT NULL CHECK (min_quantity >= 0),
    reorder_quantity  INTEGER NOT NULL CHECK (reorder_quantity >= 0),
    is_active         BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_threshold_tenant_product_location UNIQUE (tenant_id, product_id, location_id)
);
CREATE INDEX idx_threshold_tenant_id ON reorder_thresholds (tenant_id);

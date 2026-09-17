-- V5__sales_schema.sql

CREATE TABLE csv_import_batches (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    filename       VARCHAR(255) NOT NULL,
    status         VARCHAR(30) NOT NULL
                       CHECK (status IN ('PROCESSING','COMPLETED','COMPLETED_WITH_ERRORS','FAILED')),
    row_count      INTEGER NOT NULL DEFAULT 0,
    success_count  INTEGER NOT NULL DEFAULT 0,
    error_count    INTEGER NOT NULL DEFAULT 0,
    created_by     UUID NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_import_batch_tenant ON csv_import_batches (tenant_id);

CREATE TABLE csv_import_row_errors (
    id          UUID PRIMARY KEY,
    batch_id    UUID NOT NULL REFERENCES csv_import_batches(id),
    row_number  INTEGER NOT NULL,
    message     VARCHAR(1000) NOT NULL
);
CREATE INDEX idx_import_row_errors_batch ON csv_import_row_errors (batch_id);

CREATE TABLE sales (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    product_id       UUID NOT NULL REFERENCES products(id),
    location_id      UUID NOT NULL REFERENCES locations(id),
    quantity         INTEGER NOT NULL CHECK (quantity > 0),
    unit_price       NUMERIC(12, 2) NOT NULL,
    sold_at          TIMESTAMPTZ NOT NULL,
    source           VARCHAR(20) NOT NULL CHECK (source IN ('MANUAL','CSV_IMPORT')),
    import_batch_id  UUID REFERENCES csv_import_batches(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sales_tenant ON sales (tenant_id);
CREATE INDEX idx_sales_product_location ON sales (product_id, location_id);
CREATE INDEX idx_sales_sold_at ON sales (sold_at);
CREATE INDEX idx_sales_import_batch ON sales (import_batch_id);

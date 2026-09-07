-- V3__catalog_schema.sql
-- Introduces the catalog: Location, Supplier, Product.
-- All three are tenant-scoped (every table carries tenant_id + an index on it).

CREATE TABLE locations (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_locations_tenant_id ON locations (tenant_id);

CREATE TABLE suppliers (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    name          VARCHAR(255) NOT NULL,
    contact_info  VARCHAR(500),
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_suppliers_tenant_id ON suppliers (tenant_id);

CREATE TABLE products (
    id           UUID PRIMARY KEY,
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    sku          VARCHAR(100) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    supplier_id  UUID REFERENCES suppliers(id),
    unit_cost    NUMERIC(12, 2),
    unit_price   NUMERIC(12, 2),
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- SKU only needs to be unique WITHIN a tenant, not globally.
    CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku)
);
CREATE INDEX idx_products_tenant_id ON products (tenant_id);
CREATE INDEX idx_products_supplier_id ON products (supplier_id);

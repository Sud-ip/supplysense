-- V2__auth_schema.sql
-- Introduces Tenant and User, the root of multi-tenancy and RBAC.

CREATE TABLE tenants (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'MANAGER', 'STAFF')),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Email is GLOBALLY unique, not per-tenant. See interview note: a user
    -- account belongs to exactly one tenant in v1. Login is by email alone,
    -- which only works if email unambiguously identifies one account.
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);

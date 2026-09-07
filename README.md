# SupplySense — Architecture & Planning Document (v1)

Status: **Planning only. No implementation started.**

---

## 1. Requirements

### 1.1 Functional Requirements (MVP)

| # | Requirement |
|---|---|
| F1 | Multi-tenant user authentication (email/password) with JWT |
| F2 | Role-based access control: Owner, Manager, Staff |
| F3 | CRUD for Products, Suppliers, Locations |
| F4 | Immutable stock ledger — every inventory change is an append-only transaction |
| F5 | Inventory balance is a *projection* derived from the ledger, not an independently editable field |
| F6 | Sales recording, including CSV import |
| F7 | Manual reorder thresholds per product/location |
| F8 | Baseline forecasting (moving average / exponential smoothing) |
| F9 | Low-stock alerts derived from forecast + threshold |
| F10 | Dashboard: stock trends, top-moving products, at-risk products |
| F11 | Centralized exception handling, consistent error contract |
| F12 | Health checks and structured logging |

**Interview explanation:** MVP scope is deliberately restricted to what directly 
answers the five business questions in the brief. 
Nothing here requires ML, async processing, or infra beyond a single Postgres-backed 
Spring Boot app. This keeps "correctness before complexity" enforceable — we can prove the ledger
and forecasting math are correct before adding operational complexity around them.

### 1.2 Functional Requirements (Phase 2)

- Trained ML demand forecasting (LightGBM/XGBoost) replacing/augmenting baseline
- Model-vs-baseline evaluation (backtesting, accuracy comparison)
- Cold-start handling (new products/locations with insufficient history)
- Scheduled retraining
- Forecast performance monitoring
- Async CSV processing (large files)
- Async forecast job execution
- Rate limiting
- Full CI/CD pipeline

### 1.3 Non-Functional Requirements

- **Tenant isolation**: no tenant can ever read/write another tenant's data, enforced at the query layer, not just the UI.
- **Auditability**: stock and inventory-affecting mutations must be traceable to a user, timestamp, and reason.
- **Data integrity**: inventory balances must never drift from the ledger. This is a database-transaction guarantee, not an application-level "best effort."
- **Scale target**: 50–5,000 SKUs, 1–10 locations, single-digit tenants-per-instance in early days, growing to hundreds. This explicitly does **not** need horizontal event streaming or distributed caching yet.
- **Observability**: every service must expose health, metrics, and structured logs from day one — this is cheap now and expensive to retrofit.

**Interview explanation:** Non-functionals were picked to match the *actual* scale target (thousands of SKUs, not billions of events). This is why Kafka/Kubernetes/Redis/Elasticsearch are explicitly out of scope — none of these problems (fan-out event processing, container orchestration at scale, sub-millisecond caching, full-text search at scale) exist yet. Introducing them now would be complexity added for résumé value, not for the business.

---

## 2. High-Level Architecture

### 2.1 Style: **Modular Monolith**

```
┌─────────────────────────────────────────────────────┐
│                   React SPA (TS)                     │
│        Redux Toolkit · Axios · Recharts               │
└───────────────────────┬───────────────────────────────┘
                         │ HTTPS / JWT
┌───────────────────────▼───────────────────────────────┐
│               Spring Boot Application                 │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │
│  │   Web Layer  │ │  Service    │ │   Repository     │  │
│  │ (Controllers)│→│  Layer      │→│   (Spring Data   │  │
│  │   + DTOs     │ │ (Business   │ │   JPA)           │  │
│  │              │ │  Logic)     │ │                  │  │
│  └─────────────┘ └─────────────┘ └─────────────────┘  │
│  Cross-cutting: Security filter chain, exception       │
│  handler, tenant context resolver, auditing, logging    │
└───────────────────────┬───────────────────────────────┘
                         │ JDBC
                ┌────────▼────────┐
                │   PostgreSQL     │
                └──────────────────┘

Phase 2 addition (internal only, never public):
┌─────────────────────┐        ┌──────────────────────┐
│ Spring Boot backend  │──────▶│ FastAPI ML inference   │
│ (internal network    │  HTTP  │ service (forecasting)  │
│  call only)          │        │                        │
└─────────────────────┘        └──────────────────────┘
```

**Interview explanation:** A modular monolith (single deployable, internally organized by domain module — `product`, `inventory`, `sales`, `forecasting`, `auth`) is chosen over microservices. At 50–5,000 SKUs and single-digit-to-low-hundreds of tenants, the operational cost of microservices (service discovery, distributed tracing, network failure handling, data consistency across services) massively outweighs the benefit. A monolith with clean module boundaries can be split later *if and when* a specific module (most likely: ML inference) needs independent scaling or a different language runtime — which is exactly why the ML service is the one exception, isolated from day one behind an internal-only network boundary.

### 2.2 Internal Module Boundaries (within the monolith)

- `auth` — tenant, user, role, JWT issuance
- `catalog` — product, supplier, location
- `inventory` — stock ledger, balance projection, reorder thresholds
- `sales` — sales records, CSV import
- `forecasting` — baseline algorithm, (later) ML client
- `alerting` — low-stock detection
- `dashboard` — read-optimized aggregation queries
- `common` — exception handling, auditing, tenant context, DTO mapping conventions

**Interview explanation:** These module boundaries are chosen to mirror likely future service boundaries without forcing premature network separation. Each module should be internally cohesive enough that, if `forecasting` ever needs to become a separate deployable, the extraction is a refactor, not a rewrite.

---

## 3. Domain Model

### 3.1 Core Entities

```
Tenant
 ├── User (role: OWNER | MANAGER | STAFF)
 ├── Location
 ├── Supplier
 ├── Product
 │     ├── belongs to Supplier (optional)
 │     └── ReorderThreshold (per Product+Location)
 ├── StockLedgerEntry (immutable)
 │     ├── Product
 │     ├── Location
 │     ├── quantity delta (+/-)
 │     ├── reason (PURCHASE, SALE, ADJUSTMENT, RETURN, TRANSFER)
 │     ├── reference (e.g., sale id, import batch id)
 │     ├── created_by (User)
 │     └── created_at
 ├── InventoryBalance (derived/projected, one row per Product+Location)
 ├── Sale
 │     ├── Product
 │     ├── Location
 │     ├── quantity, unit_price, sold_at
 │     └── source (MANUAL | CSV_IMPORT)
 └── ForecastResult (per Product+Location, generated by baseline or ML)
```

### 3.2 Key Domain Rules

1. **StockLedgerEntry is append-only.** No update, no delete. Corrections are made via new offsetting entries (reason = ADJUSTMENT), preserving full history.
2. **InventoryBalance is never written directly by user action.** It is recalculated/updated only as a side effect of a ledger entry being inserted, in the *same DB transaction*.
3. **A Sale always produces exactly one StockLedgerEntry** (reason = SALE, negative delta). This is enforced in the service layer, not left to caller discretion.
4. **ReorderThreshold is per Product+Location** — a distributor with 5 warehouses may need different thresholds per site.
5. **Tenant is the root of every query.** Every entity (directly or via parent) carries a `tenant_id`. There is no cross-tenant entity.

**Interview explanation:** The ledger/projection split (rules 1–2) is the single most important design decision in the system. It's an event-sourcing-*lite* pattern: instead of allowing `inventory.quantity` to be mutated directly (which is how bugs and unauditable discrepancies creep into inventory systems), every change is a recorded fact, and current state is always derivable by replaying/aggregating those facts. This satisfies principle #9 and #10 directly, and gives us "why is this number what it is" for free — critical for a system whose entire value proposition is trustworthy numbers.

---

## 4. Database Design (PostgreSQL, via Flyway migrations)

### 4.1 Table Sketch (not final DDL — for review)

```sql
tenants (id, name, created_at)

users (id, tenant_id FK, email, password_hash, role, is_active, created_at)
  UNIQUE(tenant_id, email)

locations (id, tenant_id FK, name, address, is_active, created_at)

suppliers (id, tenant_id FK, name, contact_info, is_active, created_at)

products (id, tenant_id FK, sku, name, supplier_id FK NULL,
          unit_cost, unit_price, is_active, created_at)
  UNIQUE(tenant_id, sku)

reorder_thresholds (id, tenant_id FK, product_id FK, location_id FK,
                     min_quantity, reorder_quantity, updated_at)
  UNIQUE(tenant_id, product_id, location_id)

stock_ledger_entries (id, tenant_id FK, product_id FK, location_id FK,
                       quantity_delta, reason, reference_type, reference_id,
                       created_by FK -> users, created_at)
  -- append-only: no UPDATE/DELETE grants at DB role level in production

inventory_balances (id, tenant_id FK, product_id FK, location_id FK,
                     quantity_on_hand, last_ledger_id FK, updated_at)
  UNIQUE(tenant_id, product_id, location_id)

sales (id, tenant_id FK, product_id FK, location_id FK,
       quantity, unit_price, sold_at, source, import_batch_id NULL, created_at)

csv_import_batches (id, tenant_id FK, filename, status, row_count,
                     error_count, created_by FK, created_at)

forecast_results (id, tenant_id FK, product_id FK, location_id FK,
                   method (BASELINE_MA | BASELINE_ES | ML_MODEL),
                   forecast_horizon_days, predicted_daily_demand,
                   projected_stockout_date, generated_at)
```

### 4.2 Integrity & Concurrency Rules

- `inventory_balances` update happens inside the **same transaction** as the `stock_ledger_entries` insert, using either:
    - a `SELECT ... FOR UPDATE` on the balance row before applying the delta, or
    - a single atomic `UPDATE ... SET quantity_on_hand = quantity_on_hand + ?` with the ledger insert in the same transaction.
- Every tenant-scoped table has a composite index on `(tenant_id, ...)` for the columns used in lookups, so tenant filtering is always index-assisted, never a table scan.
- All FKs are scoped *within* tenant (enforced at the application/service layer plus DB constraint where practical) — a product row can never reference a location row from a different tenant.

**Interview explanation:** The `SELECT ... FOR UPDATE` / atomic-increment choice is a concurrency correctness question: two simultaneous sales against the same product/location must not race and produce a wrong balance. We'll pick the atomic-increment approach as the default (simpler, less lock contention) and only escalate to explicit row locking if a specific workflow (e.g., multi-step stock transfer) needs it. This is decided at implementation time for that specific milestone, with rationale documented then.

### 4.3 Auditability

- `stock_ledger_entries.created_by` + `created_at` give full attribution for every inventory-affecting action.
- `csv_import_batches` gives traceability for bulk sales imports (who imported what, how many rows succeeded/failed).
- Later (not MVP-blocking): a generic `audit_log` table for non-ledger sensitive actions (e.g., user role changes) can be added if a real need appears — not built preemptively.

---

## 5. API Contracts (Draft — endpoint inventory, not full specs yet)

All endpoints are prefixed `/api/v1`, require `Authorization: Bearer <JWT>` except `/auth/*`.

```
POST   /api/v1/auth/register        (creates tenant + first Owner user)
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh

GET    /api/v1/products
POST   /api/v1/products                    [OWNER, MANAGER]
GET    /api/v1/products/{id}
PUT    /api/v1/products/{id}                [OWNER, MANAGER]
DELETE /api/v1/products/{id}                [OWNER]           (soft delete)

GET    /api/v1/suppliers  ...
GET    /api/v1/locations  ...

GET    /api/v1/inventory/balances?locationId=&productId=
POST   /api/v1/inventory/adjustments        [OWNER, MANAGER]   (creates ledger entry)
GET    /api/v1/inventory/ledger?productId=&locationId=&from=&to=

POST   /api/v1/sales                        [OWNER, MANAGER, STAFF]
POST   /api/v1/sales/import                 [OWNER, MANAGER]   (CSV upload)
GET    /api/v1/sales/import/{batchId}       (status/results)

GET    /api/v1/reorder-thresholds
PUT    /api/v1/reorder-thresholds/{id}      [OWNER, MANAGER]

GET    /api/v1/forecasts?productId=&locationId=
GET    /api/v1/alerts/low-stock

GET    /api/v1/dashboard/summary
GET    /api/v1/dashboard/trends
GET    /api/v1/dashboard/top-movers

GET    /actuator/health
GET    /actuator/prometheus
```

**Interview explanation:** DTOs are used exclusively at the boundary (request DTOs in, response DTOs out) — entities never leave the service layer. This satisfies principles #6 and #7: it decouples the API contract from schema evolution, prevents accidental exposure of internal fields (e.g., `password_hash`, `tenant_id` when not needed), and lets us version the API independently of the domain model.

### 5.1 Standard Error Contract

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "quantity must be greater than 0",
  "path": "/api/v1/inventory/adjustments",
  "traceId": "..."
}
```

Handled centrally via `@RestControllerAdvice` — no controller ever hand-rolls an error response.

---

## 6. Security Model

- **Authentication**: JWT (access + refresh token pair), signed with a server-held secret (never hardcoded — sourced from environment/secret manager).
- **Authorization**: Role hierarchy `OWNER > MANAGER > STAFF`, enforced via Spring Security method security (`@PreAuthorize`) at the service layer, not just controller annotations — so authorization can't be bypassed by an internal call path.
- **Tenant isolation**: every authenticated request carries `tenant_id` (embedded in the JWT claims). A request-scoped `TenantContext` is populated by a security filter and every repository query is required to filter by it — enforced via a base repository pattern / Hibernate filter, not left to each query author's discipline.
- **Password storage**: bcrypt, never plaintext, never reversible.
- **Secrets**: DB credentials, JWT signing key, and (Phase 2) ML service credentials are injected via environment variables / managed secret store — never committed, never hardcoded.

**Interview explanation:** Putting tenant filtering at the Hibernate/repository layer (e.g., a `@FilterDef` tenant filter, or a `TenantAwareRepository` base class) rather than trusting each service method to remember `WHERE tenant_id = ?` is a deliberate defense against the single most damaging class of bug in a multi-tenant system: a forgotten tenant filter that leaks one customer's data to another. We treat this as a framework-enforced guarantee, not a code-review checklist item.

---

## 7. ML / Forecasting Strategy

### 7.1 MVP: Statistical Baseline (in-JVM, no separate service)

- Moving Average and/or Exponential Smoothing computed directly in the `forecasting` module (Java), against ledger/sales history.
- Output: predicted daily demand → projected days-to-stockout → feeds low-stock alerts.
- No Python, no separate service, no model training in MVP.

**Interview explanation:** A statistical baseline living inside the monolith is intentional — it proves the *product* concept (forecast-driven vs threshold-only alerting) without any ML infrastructure. It also becomes the benchmark that any later ML model must beat before we trust it in production, which directly serves principle #1 (correctness before complexity).

### 7.2 Phase 2: Trained ML Models

- Python service (pandas, scikit-learn, LightGBM/XGBoost as justified by actual forecasting accuracy gains — not by default).
- Exposed via **FastAPI**, reachable **only on an internal network** (never a public endpoint) — the Spring Boot backend is the sole caller.
- Model-vs-baseline evaluation: every new model is backtested against historical data and must outperform the existing baseline on a defined metric (e.g., MAPE) before it's promoted to serve live forecasts.
- Cold-start handling: products/locations with insufficient history fall back to baseline or a category-level average — never a raw ML prediction on near-zero data.
- Scheduled retraining + forecast performance monitoring: introduced only once there's a live model to maintain.

**Interview explanation:** FastAPI is introduced *specifically because* Python's ML ecosystem (pandas/scikit-learn/LightGBM) is the right tool for training and serving these models — this is the one deliberate exception to "don't add a service without justification," and even then it's kept internal-only per principle #13, called synchronously or asynchronously by the monolith rather than exposed to the internet or to the frontend directly.

---

## 8. Development Milestones

Work proceeds **milestone by milestone**, each requiring explicit approval before the next begins.

| Milestone | Scope |
|---|---|
| **M0** | Project scaffolding: Spring Boot app skeleton, Flyway setup, Docker Compose (app + Postgres), base package structure, Actuator health check. *No business logic yet.* |
| **M1** | Auth & multi-tenancy: Tenant/User entities, registration, login, JWT issuance, Spring Security config, role model, tenant context filter. |
| **M2** | Catalog: Product, Supplier, Location CRUD with DTOs, validation, RBAC-protected endpoints, tests. |
| **M3** | Inventory core: StockLedgerEntry + InventoryBalance, transactional balance projection, manual adjustment endpoint, ledger query endpoint. |
| **M4** | Sales: manual sale entry (ledger integration), CSV import (synchronous for MVP), reorder thresholds. |
| **M5** | Forecasting baseline + low-stock alerts. |
| **M6** | Dashboard endpoints (trends, top movers, summary) + centralized exception handling polish + structured logging + metrics. |
| **M7** | Frontend: React/TS shell, auth flow, product/inventory/sales screens, dashboard with Recharts. |
| **M8 (Phase 2 gate)** | Re-evaluate: async CSV/forecast jobs, ML service, rate limiting, CI/CD — each justified individually before being added. |

**Interview explanation:** Milestones are sequenced so each one is independently demoable and testable — auth before catalog (nothing works without a tenant), catalog before inventory (ledger entries need products/locations to reference), inventory before sales (a sale is defined as a ledger-producing event), and forecasting last in MVP because it depends on real sales history existing first. This ordering also means if priorities shift, we always have a working, tested increment rather than a half-built system.

---

## Open Questions for Project Owner

1. Should `STAFF` role be allowed to record sales but not view cost/margin data (unit_cost)? This affects DTO field-level visibility per role.
2. CSV import — do we need a defined column-mapping template now, or should the importer support flexible column mapping in MVP?
3. Should soft-deleted products still appear in historical ledger/dashboard queries (they should, for integrity) — confirming this is expected, not a bug, before we build it.
4. Any existing brand/design constraints for the frontend, or is Tailwind's default palette a fine starting point?

---

*This document is the baseline for implementation. Nothing beyond M0 begins without explicit sign-off.*
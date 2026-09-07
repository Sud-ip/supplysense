-- V1__baseline.sql
--
-- M0 baseline migration. Deliberately empty of domain tables - this exists
-- only to prove Flyway is correctly wired to the datasource and that the
-- migration pipeline works before any real schema is introduced.
--
-- Real domain tables (tenants, users, products, stock_ledger_entries, ...)
-- start arriving in M1's migrations (V2__..., V3__..., one logical change
-- per migration file, never edited after being committed).

SELECT 1;

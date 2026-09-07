package com.supplysense.backend.inventory.repository;

import com.supplysense.backend.inventory.domain.StockLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Extends the bare Spring Data {@code Repository<T, ID>} marker interface,
 * NOT JpaRepository or CrudRepository. This means the only operations
 * available on stock ledger entries are the ones explicitly declared
 * below - there is no update(), no delete(), no deleteById() available
 * anywhere in this codebase for ledger entries. Combined with
 * StockLedgerEntry having no setters, this makes "the ledger is
 * append-only" a structural guarantee, not a code-review convention.
 */
public interface StockLedgerRepository extends Repository<StockLedgerEntry, UUID> {

    StockLedgerEntry save(StockLedgerEntry entry);

    Optional<StockLedgerEntry> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<StockLedgerEntry> findAllByTenantIdAndProductIdAndLocationIdOrderByCreatedAtDesc(
            UUID tenantId, UUID productId, UUID locationId, Pageable pageable);

    Page<StockLedgerEntry> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}

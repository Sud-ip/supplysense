package com.supplysense.backend.inventory.repository;

import com.supplysense.backend.inventory.domain.StockLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockLedgerRepository extends Repository<StockLedgerEntry, UUID> {

    StockLedgerEntry save(StockLedgerEntry entry);

    Optional<StockLedgerEntry> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<StockLedgerEntry> findAllByTenantIdAndProductIdAndLocationIdOrderByCreatedAtDesc(
            UUID tenantId, UUID productId, UUID locationId, Pageable pageable);

    Page<StockLedgerEntry> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    // Added in M6: dashboard trends need the raw entries within a window
    // to group by day in application code (see DashboardService).
    List<StockLedgerEntry> findAllByTenantIdAndCreatedAtAfter(UUID tenantId, Instant since);
}

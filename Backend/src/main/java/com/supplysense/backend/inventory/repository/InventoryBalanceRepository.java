package com.supplysense.backend.inventory.repository;

import com.supplysense.backend.inventory.domain.InventoryBalance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Also extends the bare {@code Repository<T, ID>} marker - deliberately no
 * save()/update() exposed. The ONLY way to change quantity_on_hand is
 * {@link #upsertBalanceAtomically}, an INSERT ... ON CONFLICT DO UPDATE
 * that both creates-if-absent and atomically increments-if-present in a
 * single SQL statement - immune to the classic
 * read-current-value / add-in-memory / write-back race condition that a
 * SELECT-then-UPDATE approach would have under concurrent writes.
 */
public interface InventoryBalanceRepository extends Repository<InventoryBalance, UUID> {

    Optional<InventoryBalance> findByTenantIdAndProductIdAndLocationId(
            UUID tenantId, UUID productId, UUID locationId);

    List<InventoryBalance> findAllByTenantId(UUID tenantId);

    List<InventoryBalance> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);

    @Modifying
    @Query(value = """
            INSERT INTO inventory_balances
                (id, tenant_id, product_id, location_id, quantity_on_hand, last_ledger_id, updated_at)
            VALUES
                (:id, :tenantId, :productId, :locationId, :delta, :ledgerId, now())
            ON CONFLICT (tenant_id, product_id, location_id)
            DO UPDATE SET
                quantity_on_hand = inventory_balances.quantity_on_hand + EXCLUDED.quantity_on_hand,
                last_ledger_id   = EXCLUDED.last_ledger_id,
                updated_at       = now()
            """, nativeQuery = true)
    void upsertBalanceAtomically(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId,
            @Param("locationId") UUID locationId,
            @Param("delta") int delta,
            @Param("ledgerId") UUID ledgerId);
}

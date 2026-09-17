package com.supplysense.backend.sales.repository;

import com.supplysense.backend.sales.domain.Sale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends Repository<Sale, UUID> {

    Sale save(Sale sale);

    @Query("""
            SELECT COALESCE(SUM(s.quantity), 0)
            FROM Sale s
            WHERE s.tenant.id = :tenantId
              AND s.product.id = :productId
              AND s.location.id = :locationId
              AND s.soldAt >= :since
            """)
    int sumQuantitySoldSince(
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId,
            @Param("locationId") UUID locationId,
            @Param("since") Instant since);

    // Added in M6: top-movers dashboard widget. GROUP BY + ORDER BY on an
    // aggregate is plain, portable JPQL - no native SQL needed here,
    // unlike the atomic upsert in InventoryBalanceRepository which
    // genuinely required Postgres-specific syntax.
    @Query("""
            SELECT s.product.id AS productId, SUM(s.quantity) AS totalSold
            FROM Sale s
            WHERE s.tenant.id = :tenantId
              AND s.soldAt >= :since
            GROUP BY s.product.id
            ORDER BY SUM(s.quantity) DESC
            """)
    List<TopMoverRow> findTopMoversSince(
            @Param("tenantId") UUID tenantId,
            @Param("since") Instant since,
            Pageable pageable);

    interface TopMoverRow {
        UUID getProductId();
        long getTotalSold();
    }
}

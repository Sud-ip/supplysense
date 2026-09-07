package com.supplysense.backend.inventory.domain;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A projection: "as of the last ledger entry applied, this product at
 * this location has this many units on hand." This entity exists so we
 * can READ current stock cheaply (one row lookup instead of summing the
 * entire ledger every time) - it is never the source of truth, the
 * ledger is. See InventoryBalanceRepository: there is no JPA-level
 * save()/update() exposed for this entity's quantityOnHand - the only
 * write path is the native atomic upsert query, always issued in the
 * same transaction as a ledger insert.
 */
@Entity
@Table(name = "inventory_balances")
public class InventoryBalance {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "last_ledger_id")
    private UUID lastLedgerId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryBalance() {
        // JPA - and the native upsert query, which bypasses this
        // constructor entirely and writes columns directly.
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Product getProduct() {
        return product;
    }

    public Location getLocation() {
        return location;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public UUID getLastLedgerId() {
        return lastLedgerId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

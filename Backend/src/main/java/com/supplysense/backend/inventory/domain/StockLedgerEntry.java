package com.supplysense.backend.inventory.domain;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A single, immutable fact: "this many units of this product at this
 * location changed by this amount, for this reason, at this time,
 * recorded by this user."
 *
 * IMMUTABILITY IS ENFORCED HERE, NOT JUST BY CONVENTION: there are no
 * setters on this class. Once an instance is constructed and persisted,
 * there is no method available in Java to change any of its fields -
 * even if some future code fetched an entry and called
 * repository.save(entry) on it, there would be nothing to have modified
 * first. This, combined with StockLedgerRepository exposing no
 * update/delete methods, is what makes principle #9 real rather than
 * aspirational.
 */
@Entity
@Table(name = "stock_ledger_entries")
public class StockLedgerEntry {

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

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockChangeReason reason;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockLedgerEntry() {
        // JPA
    }

    public StockLedgerEntry(
            Tenant tenant,
            Product product,
            Location location,
            int quantityDelta,
            StockChangeReason reason,
            String referenceType,
            UUID referenceId,
            User createdBy
    ) {
        if (quantityDelta == 0) {
            throw new IllegalArgumentException("quantityDelta must not be zero");
        }
        this.tenant = tenant;
        this.product = product;
        this.location = location;
        this.quantityDelta = quantityDelta;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // ---- Getters only. No setters. This is intentional. ----

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

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public StockChangeReason getReason() {
        return reason;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

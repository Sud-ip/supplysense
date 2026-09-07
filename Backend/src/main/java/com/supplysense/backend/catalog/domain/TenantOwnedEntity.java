package com.supplysense.backend.catalog.domain;

import com.supplysense.backend.auth.domain.Tenant;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Every catalog (and later inventory/sales) entity extends this. It
 * guarantees three things structurally, rather than by convention:
 *  1. Every row belongs to exactly one tenant (non-null, non-optional FK).
 *  2. Every row is soft-deletable (isActive), never hard-deleted - so
 *     historical references (ledger entries, sales, forecasts) never
 *     point at a vanished row.
 *  3. Every row records when it was created.
 *
 * @MappedSuperclass (not @Entity/@Inheritance) means this contributes
 * columns to each subclass's own table - there's no shared "base" table
 * and no join overhead. Each of locations/suppliers/products stays a
 * normal, independent table.
 */
@MappedSuperclass
public abstract class TenantOwnedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    protected void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

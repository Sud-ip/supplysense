package com.supplysense.backend.sales.domain;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, same reasoning as StockLedgerEntry: no setters. A recorded
 * sale is a historical fact; if it was wrong, the fix is a new adjusting
 * ledger entry (M3's ADJUSTMENT/RETURN reasons), not an edit to this row.
 */
@Entity
@Table(name = "sales")
public class Sale {

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

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleSource source;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Sale() {
        // JPA
    }

    public Sale(
            Tenant tenant,
            Product product,
            Location location,
            int quantity,
            BigDecimal unitPrice,
            Instant soldAt,
            SaleSource source,
            UUID importBatchId
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.tenant = tenant;
        this.product = product;
        this.location = location;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.soldAt = soldAt;
        this.source = source;
        this.importBatchId = importBatchId;
    }

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

    public Product getProduct() {
        return product;
    }

    public Location getLocation() {
        return location;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Instant getSoldAt() {
        return soldAt;
    }

    public SaleSource getSource() {
        return source;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.supplysense.backend.inventory.domain;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.domain.TenantOwnedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "reorder_thresholds")
public class ReorderThreshold extends TenantOwnedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    @Column(name = "reorder_quantity", nullable = false)
    private int reorderQuantity;

    protected ReorderThreshold() {
        // JPA
    }

    public ReorderThreshold(Tenant tenant, Product product, Location location, int minQuantity, int reorderQuantity) {
        setTenant(tenant);
        this.product = product;
        this.location = location;
        this.minQuantity = minQuantity;
        this.reorderQuantity = reorderQuantity;
    }

    public Product getProduct() {
        return product;
    }

    public Location getLocation() {
        return location;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(int minQuantity) {
        this.minQuantity = minQuantity;
    }

    public int getReorderQuantity() {
        return reorderQuantity;
    }

    public void setReorderQuantity(int reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }
}

package com.supplysense.backend.catalog.domain;

import com.supplysense.backend.auth.domain.Tenant;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends TenantOwnedEntity {

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected Product() {
        // JPA
    }

    public Product(Tenant tenant, String sku, String name, Supplier supplier,
                    BigDecimal unitCost, BigDecimal unitPrice) {
        setTenant(tenant);
        this.sku = sku;
        this.name = name;
        this.supplier = supplier;
        this.unitCost = unitCost;
        this.unitPrice = unitPrice;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}

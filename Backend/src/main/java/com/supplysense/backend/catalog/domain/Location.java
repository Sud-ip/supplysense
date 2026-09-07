package com.supplysense.backend.catalog.domain;

import com.supplysense.backend.auth.domain.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Location extends TenantOwnedEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    protected Location() {
        // JPA
    }

    public Location(Tenant tenant, String name, String address) {
        setTenant(tenant);
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

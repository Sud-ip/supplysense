package com.supplysense.backend.catalog.domain;

import com.supplysense.backend.auth.domain.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class Supplier extends TenantOwnedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_info")
    private String contactInfo;

    protected Supplier() {
        // JPA
    }

    public Supplier(Tenant tenant, String name, String contactInfo) {
        setTenant(tenant);
        this.name = name;
        this.contactInfo = contactInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}

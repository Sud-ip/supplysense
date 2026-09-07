package com.supplysense.backend.auth.domain;

/**
 * OWNER > MANAGER > STAFF, enforced via Spring Security's role hierarchy
 * and @PreAuthorize at the service layer (added as each module needs it,
 * starting M2 for catalog write operations).
 */
public enum Role {
    OWNER,
    MANAGER,
    STAFF
}

package com.supplysense.backend.security;

import java.util.UUID;

/**
 * Holds the current request's tenantId/userId/role, populated by
 * {@link JwtAuthenticationFilter} after successful token validation and
 * cleared at the end of every request.
 *
 * Starting M2, every tenant-scoped repository query will read from this
 * context rather than trusting a caller-supplied tenantId - this is the
 * enforcement point for tenant isolation described in the security model.
 * Using a ThreadLocal (not a request-scoped Spring bean) keeps it usable
 * from repository/Hibernate-filter code that isn't itself a Spring bean.
 */
public final class TenantContext {

    private static final ThreadLocal<AuthenticatedPrincipal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(AuthenticatedPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AuthenticatedPrincipal get() {
        AuthenticatedPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new IllegalStateException(
                    "TenantContext accessed outside an authenticated request");
        }
        return principal;
    }

    public static UUID currentTenantId() {
        return get().tenantId();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record AuthenticatedPrincipal(UUID userId, UUID tenantId, String email, String role) {
    }
}

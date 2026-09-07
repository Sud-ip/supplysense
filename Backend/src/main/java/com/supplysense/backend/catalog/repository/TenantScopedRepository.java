package com.supplysense.backend.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every catalog repository extends this instead of plain JpaRepository.
 *
 * Interview explanation: we chose EXPLICIT tenant-filtered query methods
 * (every call takes a tenantId argument) over a global Hibernate @Filter
 * that auto-applies "WHERE tenant_id = ?" behind the scenes. A global
 * filter is less code at the call site, but it fails silently if a
 * session ever forgets to enable it - a single missed filter activation
 * leaks one tenant's data to another with no compiler or test warning.
 * Explicit tenantId parameters make every query's tenant-safety visible
 * and greppable in code review, at the cost of a little repetition. Given
 * that tenant isolation is a hard requirement (not a nice-to-have), we're
 * trading brevity for auditability here on purpose.
 *
 * Every service method that calls these MUST pass
 * TenantContext.currentTenantId() - never a tenantId taken from a
 * request body or path variable.
 */
@NoRepositoryBean
public interface TenantScopedRepository<T, ID> extends JpaRepository<T, ID> {

    List<T> findAllByTenantId(UUID tenantId);

    Optional<T> findByIdAndTenantId(ID id, UUID tenantId);

    boolean existsByIdAndTenantId(ID id, UUID tenantId);
}

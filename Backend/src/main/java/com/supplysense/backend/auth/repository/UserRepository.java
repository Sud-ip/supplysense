package com.supplysense.backend.auth.repository;

import com.supplysense.backend.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Added for the "OWNER invites MANAGER/STAFF" flow: list everyone in
    // the current tenant.
    List<User> findAllByTenantId(UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}

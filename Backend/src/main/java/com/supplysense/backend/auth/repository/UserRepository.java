package com.supplysense.backend.auth.repository;

import com.supplysense.backend.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Email is globally unique (see V2 migration note), so lookup by email
    // alone is sufficient to identify the account and its tenant - no
    // tenant_id needed on this query, unlike every other repository we'll
    // write from M2 onward, which will always filter by tenant.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}

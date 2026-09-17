package com.supplysense.backend.auth.service;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.dto.CreateUserRequest;
import com.supplysense.backend.auth.dto.UserResponse;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.common.exception.DuplicateEmailException;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.security.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Missing piece from M1: registration always creates a Tenant + OWNER
 * together, but there was never a way for that OWNER to add MANAGER or
 * STAFF users afterward. This closes that gap.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * OWNER-only: invites a MANAGER or STAFF user into the OWNER's own
     * tenant. Deliberately does not allow creating another OWNER through
     * this endpoint - registration (POST /auth/register) is the only way
     * a new OWNER comes into existence, keeping "who owns this tenant"
     * unambiguous. If co-ownership becomes a real need later, that's a
     * deliberate, separate decision to make - not a side effect of this
     * endpoint's validation being loose.
     */
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.role() == Role.OWNER) {
            throw new IllegalArgumentException(
                    "Cannot create additional OWNER users via this endpoint");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        UUID tenantId = TenantContext.currentTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        User user = new User(
                tenant, request.email(), passwordEncoder.encode(request.password()),
                request.fullName(), request.role());

        return UserResponse.from(userRepository.save(user));
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        UUID tenantId = TenantContext.currentTenantId();
        return userRepository.findAllByTenantId(tenantId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public void deactivateUser(UUID userId) {
        UUID tenantId = TenantContext.currentTenantId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRole() == Role.OWNER) {
            throw new IllegalArgumentException("Cannot deactivate the tenant's OWNER");
        }

        user.deactivate();
    }
}

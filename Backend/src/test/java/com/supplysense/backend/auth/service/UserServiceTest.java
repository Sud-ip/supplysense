package com.supplysense.backend.auth.service;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.dto.CreateUserRequest;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock TenantRepository tenantRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.AuthenticatedPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), "owner@test.example", "OWNER"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rejectsCreatingAnotherOwnerThroughThisEndpoint() {
        CreateUserRequest request = new CreateUserRequest("Co Owner", "co@test.example", "password123", Role.OWNER);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class);

        // Rejected before touching the database at all.
        verifyNoInteractions(userRepository, tenantRepository, passwordEncoder);
    }
}

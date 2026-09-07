package com.supplysense.backend.auth.service;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.dto.LoginRequest;
import com.supplysense.backend.auth.dto.RegisterRequest;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.common.exception.DuplicateEmailException;
import com.supplysense.backend.common.exception.InvalidCredentialsException;
import com.supplysense.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks
    AuthService authService;

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@acme.test")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Acme Co", "Jane Owner", "taken@acme.test", "password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesTenantAndOwnerAndIssuesTokens() {
        when(userRepository.existsByEmail("new@acme.test")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.accessTokenExpirationSeconds()).thenReturn(900L);

        RegisterRequest request = new RegisterRequest("Acme Co", "Jane Owner", "new@acme.test", "password123");
        var response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.role()).isEqualTo(Role.OWNER);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void loginRejectsWrongPassword() {
        Tenant tenant = new Tenant("Acme Co");
        User user = new User(tenant, "owner@acme.test", "hashed", "Jane", Role.OWNER);
        when(userRepository.findByEmail("owner@acme.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest("owner@acme.test", "wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@acme.test")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("ghost@acme.test", "whatever");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

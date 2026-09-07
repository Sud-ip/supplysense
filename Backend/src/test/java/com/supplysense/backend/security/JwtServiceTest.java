package com.supplysense.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-for-unit-tests-only-needs-32-chars-minimum",
                15,
                7
        );
        jwtService = new JwtService(properties);
    }

    @Test
    void generatedAccessTokenContainsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, tenantId, "owner@acme.test", "OWNER");
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractTenantId(claims)).isEqualTo(tenantId);
        assertThat(jwtService.extractEmail(claims)).isEqualTo("owner@acme.test");
        assertThat(jwtService.extractRole(claims)).isEqualTo("OWNER");
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
    }

    @Test
    void refreshTokenIsMarkedAsSuch() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(userId);
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
    }

    @Test
    void tamperedTokenFailsValidation() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.com", "STAFF");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(JwtException.class);
    }
}

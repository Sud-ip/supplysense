package com.supplysense.backend.auth.dto;

import com.supplysense.backend.auth.domain.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UUID userId,
        UUID tenantId,
        String email,
        Role role
) {
}

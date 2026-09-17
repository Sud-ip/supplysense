package com.supplysense.backend.auth.dto;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getRole(), user.isActive(), user.getCreatedAt());
    }
}

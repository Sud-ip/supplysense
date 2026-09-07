package com.supplysense.backend.catalog.dto;

import com.supplysense.backend.catalog.domain.Location;

import java.time.Instant;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String address,
        boolean active,
        Instant createdAt
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(), location.getName(), location.getAddress(),
                location.isActive(), location.getCreatedAt());
    }
}

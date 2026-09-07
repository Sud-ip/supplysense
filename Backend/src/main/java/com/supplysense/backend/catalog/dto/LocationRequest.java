package com.supplysense.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record LocationRequest(
        @NotBlank(message = "name is required")
        String name,

        String address
) {
}

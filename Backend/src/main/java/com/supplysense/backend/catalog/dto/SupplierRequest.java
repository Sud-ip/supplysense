package com.supplysense.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "name is required")
        String name,

        String contactInfo
) {
}

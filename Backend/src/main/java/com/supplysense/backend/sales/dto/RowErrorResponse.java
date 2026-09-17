package com.supplysense.backend.sales.dto;

import com.supplysense.backend.sales.domain.CsvImportRowError;

public record RowErrorResponse(int rowNumber, String message) {
    public static RowErrorResponse from(CsvImportRowError error) {
        return new RowErrorResponse(error.getRowNumber(), error.getMessage());
    }
}

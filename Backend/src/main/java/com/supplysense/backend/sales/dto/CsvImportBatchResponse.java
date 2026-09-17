package com.supplysense.backend.sales.dto;

import com.supplysense.backend.sales.domain.CsvImportBatch;
import com.supplysense.backend.sales.domain.ImportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CsvImportBatchResponse(
        UUID id,
        String filename,
        ImportStatus status,
        int rowCount,
        int successCount,
        int errorCount,
        List<RowErrorResponse> errors,
        Instant createdAt
) {
    public static CsvImportBatchResponse from(CsvImportBatch batch, List<RowErrorResponse> errors) {
        return new CsvImportBatchResponse(
                batch.getId(), batch.getFilename(), batch.getStatus(),
                batch.getRowCount(), batch.getSuccessCount(), batch.getErrorCount(),
                errors, batch.getCreatedAt());
    }
}

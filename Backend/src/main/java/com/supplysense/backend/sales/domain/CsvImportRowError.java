package com.supplysense.backend.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "csv_import_row_errors")
public class CsvImportRowError {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(nullable = false, length = 1000)
    private String message;

    protected CsvImportRowError() {
        // JPA
    }

    public CsvImportRowError(UUID batchId, int rowNumber, String message) {
        this.batchId = batchId;
        this.rowNumber = rowNumber;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getMessage() {
        return message;
    }
}

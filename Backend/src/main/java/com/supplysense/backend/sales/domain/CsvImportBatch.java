package com.supplysense.backend.sales.domain;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Unlike Sale/StockLedgerEntry, this entity IS legitimately mutable - it
 * tracks in-progress state (PROCESSING -> COMPLETED/FAILED). But mutation
 * is restricted to exactly one method, complete(...), rather than open
 * setters - this is still a controlled state transition, not free-form
 * editing.
 */
@Entity
@Table(name = "csv_import_batches")
public class CsvImportBatch {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportStatus status;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CsvImportBatch() {
        // JPA
    }

    public CsvImportBatch(Tenant tenant, String filename, User createdBy) {
        this.tenant = tenant;
        this.filename = filename;
        this.createdBy = createdBy;
        this.status = ImportStatus.PROCESSING;
        this.rowCount = 0;
        this.successCount = 0;
        this.errorCount = 0;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /** The one and only state transition this entity supports. */
    public void complete(int rowCount, int successCount, int errorCount) {
        this.rowCount = rowCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.status = errorCount == 0
                ? ImportStatus.COMPLETED
                : (successCount == 0 ? ImportStatus.FAILED : ImportStatus.COMPLETED_WITH_ERRORS);
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getFilename() {
        return filename;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

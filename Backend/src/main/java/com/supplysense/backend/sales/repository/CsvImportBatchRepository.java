package com.supplysense.backend.sales.repository;

import com.supplysense.backend.sales.domain.CsvImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CsvImportBatchRepository extends JpaRepository<CsvImportBatch, UUID> {

    Optional<CsvImportBatch> findByIdAndTenantId(UUID id, UUID tenantId);
}

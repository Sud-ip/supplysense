package com.supplysense.backend.sales.repository;

import com.supplysense.backend.sales.domain.CsvImportRowError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CsvImportRowErrorRepository extends JpaRepository<CsvImportRowError, UUID> {

    List<CsvImportRowError> findAllByBatchIdOrderByRowNumber(UUID batchId);
}

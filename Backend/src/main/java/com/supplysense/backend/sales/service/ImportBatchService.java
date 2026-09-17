package com.supplysense.backend.sales.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.sales.domain.CsvImportBatch;
import com.supplysense.backend.sales.domain.CsvImportRowError;
import com.supplysense.backend.sales.dto.CsvImportBatchResponse;
import com.supplysense.backend.sales.dto.RowErrorResponse;
import com.supplysense.backend.sales.repository.CsvImportBatchRepository;
import com.supplysense.backend.sales.repository.CsvImportRowErrorRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Deliberately separate from SalesCsvImportService: createBatch and
 * completeBatch are each their own short transaction, called from
 * outside any row-processing loop. This keeps the batch record's
 * lifecycle independent of how many rows succeed or fail.
 */
@Service
public class ImportBatchService {

    private final CsvImportBatchRepository batchRepository;
    private final CsvImportRowErrorRepository rowErrorRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public ImportBatchService(
            CsvImportBatchRepository batchRepository,
            CsvImportRowErrorRepository rowErrorRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository
    ) {
        this.batchRepository = batchRepository;
        this.rowErrorRepository = rowErrorRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CsvImportBatch createBatch(String filename) {
        UUID tenantId = TenantContext.currentTenantId();
        UUID userId = TenantContext.get().userId();

        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return batchRepository.save(new CsvImportBatch(tenant, filename, user));
    }

    @Transactional
    public void completeBatch(UUID batchId, int rowCount, int successCount, List<RowError> rowErrors) {
        CsvImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("CsvImportBatch", batchId));

        batch.complete(rowCount, successCount, rowErrors.size());

        for (RowError error : rowErrors) {
            rowErrorRepository.save(new CsvImportRowError(batchId, error.rowNumber(), error.message()));
        }
    }

    /** rowNumber is 1-based, counting the header as row 1 (matches what a user sees opening the file in a spreadsheet). */
    public record RowError(int rowNumber, String message) {
    }

    @Transactional(readOnly = true)
    public CsvImportBatchResponse getBatch(UUID batchId) {
        UUID tenantId = TenantContext.currentTenantId();
        CsvImportBatch batch = batchRepository.findByIdAndTenantId(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CsvImportBatch", batchId));

        List<RowErrorResponse> errors = rowErrorRepository.findAllByBatchIdOrderByRowNumber(batchId).stream()
                .map(RowErrorResponse::from)
                .toList();

        return CsvImportBatchResponse.from(batch, errors);
    }
}

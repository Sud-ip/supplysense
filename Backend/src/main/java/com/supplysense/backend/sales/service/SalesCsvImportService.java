package com.supplysense.backend.sales.service;

import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.sales.domain.CsvImportBatch;
import com.supplysense.backend.sales.dto.CsvImportBatchResponse;
import com.supplysense.backend.security.TenantContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fixed CSV template (MVP decision - see architecture doc's open
 * question, resolved here): a header row followed by data rows with
 * exactly these columns:
 *
 *   sku, locationName, quantity, unitPrice, soldAt
 *
 * unitPrice and soldAt may be left blank (default to product's current
 * price and "now" respectively). sku must match an existing product's
 * SKU for this tenant; locationName must match an existing location's
 * name (case-insensitive) for this tenant.
 *
 * SYNCHRONOUS by design for MVP (per the milestone plan - async CSV
 * processing is a Phase 2 item). For the 50-5,000 SKU / single-file
 * scale this product targets, a synchronous request/response is simpler
 * and gives the user immediate feedback; async job tracking becomes
 * worth the complexity only once file sizes or import frequency justify
 * it.
 */
@Service
public class SalesCsvImportService {

    private static final List<String> REQUIRED_HEADERS =
            List.of("sku", "locationName", "quantity", "unitPrice", "soldAt");

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final SaleService saleService;
    private final ImportBatchService importBatchService;

    public SalesCsvImportService(
            ProductRepository productRepository,
            LocationRepository locationRepository,
            SaleService saleService,
            ImportBatchService importBatchService
    ) {
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.saleService = saleService;
        this.importBatchService = importBatchService;
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public CsvImportBatchResponse importSalesCsv(MultipartFile file) {
        UUID tenantId = TenantContext.currentTenantId();
        UUID userId = TenantContext.get().userId();

        // Each row commits (or fails) independently via SaleService's own
        // @Transactional boundary - so this method itself is deliberately
        // NOT @Transactional. Wrapping the whole file in one transaction
        // would mean one bad row rolls back every good row before it,
        // defeating the partial-success requirement.
        CsvImportBatch batch = importBatchService.createBatch(file.getOriginalFilename());

        int rowCount = 0;
        int successCount = 0;
        List<ImportBatchService.RowError> rowErrors = new ArrayList<>();

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreSurroundingSpaces(true)
                     .build()
                     .parse(reader)) {

            validateHeaders(parser);

            for (CSVRecord record : parser) {
                rowCount++;
                int rowNumber = rowCount + 1; // +1 so the header counts as row 1, matching what a user sees in a spreadsheet

                try {
                    processRow(record, tenantId, userId, batch.getId());
                    successCount++;
                } catch (RuntimeException rowFailure) {
                    rowErrors.add(new ImportBatchService.RowError(rowNumber, rowFailure.getMessage()));
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded CSV file", e);
        }

        importBatchService.completeBatch(batch.getId(), rowCount, successCount, rowErrors);
        return importBatchService.getBatch(batch.getId());
    }

    private void validateHeaders(CSVParser parser) {
        List<String> actualHeaders = parser.getHeaderNames();
        for (String required : REQUIRED_HEADERS) {
            if (!actualHeaders.contains(required)) {
                throw new IllegalArgumentException(
                        "CSV is missing required column '" + required + "'. Expected columns: "
                                + String.join(", ", REQUIRED_HEADERS));
            }
        }
    }

    private void processRow(CSVRecord record, UUID tenantId, UUID userId, UUID batchId) {
        String sku = record.get("sku");
        String locationName = record.get("locationName");
        String quantityStr = record.get("quantity");
        String unitPriceStr = record.get("unitPrice");
        String soldAtStr = record.get("soldAt");

        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku is required");
        }
        if (locationName == null || locationName.isBlank()) {
            throw new IllegalArgumentException("locationName is required");
        }

        Product product = productRepository.findBySkuAndTenantId(sku, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU", sku));

        Location location = locationRepository.findByTenantIdAndNameIgnoreCase(tenantId, locationName)
                .orElseThrow(() -> new ResourceNotFoundException("Location named", locationName));

        int quantity = parsePositiveInt(quantityStr, "quantity");

        BigDecimal unitPrice = (unitPriceStr == null || unitPriceStr.isBlank())
                ? product.getUnitPrice()
                : parseNonNegativeDecimal(unitPriceStr, "unitPrice");

        Instant soldAt = (soldAtStr == null || soldAtStr.isBlank())
                ? Instant.now()
                : parseInstant(soldAtStr);

        saleService.recordSaleFromImport(
                product.getId(), location.getId(), quantity, unitPrice, soldAt, batchId, userId);
    }

    private int parsePositiveInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number, got '" + value + "'");
        }
    }

    private BigDecimal parseNonNegativeDecimal(String value, String fieldName) {
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException(fieldName + " must not be negative");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number, got '" + value + "'");
        }
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "soldAt must be an ISO-8601 instant (e.g. 2026-01-15T10:30:00Z), got '" + value + "'");
        }
    }
}

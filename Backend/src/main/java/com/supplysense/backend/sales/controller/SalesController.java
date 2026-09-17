package com.supplysense.backend.sales.controller;

import com.supplysense.backend.sales.dto.CsvImportBatchResponse;
import com.supplysense.backend.sales.dto.SaleRequest;
import com.supplysense.backend.sales.dto.SaleResponse;
import com.supplysense.backend.sales.service.ImportBatchService;
import com.supplysense.backend.sales.service.SaleService;
import com.supplysense.backend.sales.service.SalesCsvImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SaleService saleService;
    private final SalesCsvImportService salesCsvImportService;
    private final ImportBatchService importBatchService;

    public SalesController(
            SaleService saleService,
            SalesCsvImportService salesCsvImportService,
            ImportBatchService importBatchService
    ) {
        this.saleService = saleService;
        this.salesCsvImportService = salesCsvImportService;
        this.importBatchService = importBatchService;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> recordSale(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.recordManualSale(request));
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<CsvImportBatchResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesCsvImportService.importSalesCsv(file));
    }

    @GetMapping("/import/{batchId}")
    public CsvImportBatchResponse getImportStatus(@PathVariable UUID batchId) {
        return importBatchService.getBatch(batchId);
    }
}

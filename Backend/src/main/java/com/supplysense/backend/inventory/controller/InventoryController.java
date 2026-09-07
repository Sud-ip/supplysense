package com.supplysense.backend.inventory.controller;

import com.supplysense.backend.inventory.dto.InventoryBalanceResponse;
import com.supplysense.backend.inventory.dto.StockAdjustmentRequest;
import com.supplysense.backend.inventory.dto.StockLedgerEntryResponse;
import com.supplysense.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockLedgerEntryResponse> recordAdjustment(
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.recordManualAdjustment(request));
    }

    @GetMapping("/balances")
    public List<InventoryBalanceResponse> getBalances(
            @RequestParam(required = false) UUID productId
    ) {
        return inventoryService.getBalances(productId);
    }

    @GetMapping("/ledger")
    public Page<StockLedgerEntryResponse> getLedger(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return inventoryService.getLedgerHistory(productId, locationId, pageable);
    }
}

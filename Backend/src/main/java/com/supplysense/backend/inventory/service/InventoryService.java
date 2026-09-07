package com.supplysense.backend.inventory.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.inventory.domain.InventoryBalance;
import com.supplysense.backend.inventory.domain.StockChangeReason;
import com.supplysense.backend.inventory.domain.StockLedgerEntry;
import com.supplysense.backend.inventory.dto.InventoryBalanceResponse;
import com.supplysense.backend.inventory.dto.StockAdjustmentRequest;
import com.supplysense.backend.inventory.dto.StockLedgerEntryResponse;
import com.supplysense.backend.inventory.repository.InventoryBalanceRepository;
import com.supplysense.backend.inventory.repository.StockLedgerRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final StockLedgerRepository stockLedgerRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public InventoryService(
            StockLedgerRepository stockLedgerRepository,
            InventoryBalanceRepository inventoryBalanceRepository,
            ProductRepository productRepository,
            LocationRepository locationRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository
    ) {
        this.stockLedgerRepository = stockLedgerRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public StockLedgerEntryResponse recordManualAdjustment(StockAdjustmentRequest request) {
        UUID tenantId = TenantContext.currentTenantId();
        UUID userId = TenantContext.get().userId();

        Product product = productRepository.findByIdAndTenantId(request.productId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        Location location = locationRepository.findByIdAndTenantId(request.locationId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", request.locationId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        StockLedgerEntry entry = recordStockChange(
                tenantId, product, location, request.quantityDelta(),
                request.reason().toStockChangeReason(),
                "MANUAL_ADJUSTMENT", null, user);

        return StockLedgerEntryResponse.from(entry);
    }

    /**
     * THE single path by which stock_ledger_entries and inventory_balances
     * are ever written, in this module or any future one. Sales (M4) and
     * any future purchasing module call this exact method - they never
     * touch either repository directly. This is what makes principles #9
     * and #10 hold for the whole system, not just for manual adjustments.
     *
     * referenceType/referenceId let a caller (e.g. SalesService) link a
     * ledger entry back to the record that caused it, without the ledger
     * needing to know about every possible calling module.
     */
    @Transactional
    public StockLedgerEntry recordStockChange(
            UUID tenantId,
            Product product,
            Location location,
            int quantityDelta,
            StockChangeReason reason,
            String referenceType,
            UUID referenceId,
            User createdBy
    ) {
        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        StockLedgerEntry entry = new StockLedgerEntry(
                tenant, product, location, quantityDelta, reason, referenceType, referenceId, createdBy);
        entry = stockLedgerRepository.save(entry);

        // Same transaction as the ledger insert above. If this line
        // throws, the ledger insert rolls back too - they succeed or
        // fail together, never independently (principle #10).
        inventoryBalanceRepository.upsertBalanceAtomically(
                UUID.randomUUID(), tenantId, product.getId(), location.getId(),
                quantityDelta, entry.getId());

        return entry;
    }

    @Transactional(readOnly = true)
    public List<InventoryBalanceResponse> getBalances(UUID productIdFilter) {
        UUID tenantId = TenantContext.currentTenantId();
        List<InventoryBalance> balances = productIdFilter != null
                ? inventoryBalanceRepository.findAllByTenantIdAndProductId(tenantId, productIdFilter)
                : inventoryBalanceRepository.findAllByTenantId(tenantId);

        return balances.stream().map(InventoryBalanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<StockLedgerEntryResponse> getLedgerHistory(UUID productId, UUID locationId, Pageable pageable) {
        UUID tenantId = TenantContext.currentTenantId();

        Page<StockLedgerEntry> page = (productId != null && locationId != null)
                ? stockLedgerRepository.findAllByTenantIdAndProductIdAndLocationIdOrderByCreatedAtDesc(
                        tenantId, productId, locationId, pageable)
                : stockLedgerRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        return page.map(StockLedgerEntryResponse::from);
    }
}

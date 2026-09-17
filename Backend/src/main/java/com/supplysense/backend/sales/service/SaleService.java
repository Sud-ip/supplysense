package com.supplysense.backend.sales.service;

import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import com.supplysense.backend.catalog.repository.LocationRepository;
import com.supplysense.backend.catalog.repository.ProductRepository;
import com.supplysense.backend.common.exception.ResourceNotFoundException;
import com.supplysense.backend.inventory.domain.StockChangeReason;
import com.supplysense.backend.inventory.service.InventoryService;
import com.supplysense.backend.sales.domain.Sale;
import com.supplysense.backend.sales.domain.SaleSource;
import com.supplysense.backend.sales.dto.SaleRequest;
import com.supplysense.backend.sales.dto.SaleResponse;
import com.supplysense.backend.sales.repository.SaleRepository;
import com.supplysense.backend.security.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public SaleService(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            LocationRepository locationRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            InventoryService inventoryService
    ) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    @PreAuthorize("hasAnyRole('OWNER','MANAGER','STAFF')")
    @Transactional
    public SaleResponse recordManualSale(SaleRequest request) {
        UUID tenantId = TenantContext.currentTenantId();
        UUID userId = TenantContext.get().userId();

        Product product = productRepository.findByIdAndTenantId(request.productId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        Location location = locationRepository.findByIdAndTenantId(request.locationId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", request.locationId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        BigDecimal unitPrice = request.unitPrice() != null ? request.unitPrice() : product.getUnitPrice();
        Instant soldAt = request.soldAt() != null ? request.soldAt() : Instant.now();

        Sale sale = createSaleAndLedgerEntry(
                tenantId, product, location, request.quantity(), unitPrice, soldAt,
                SaleSource.MANUAL, null, user);

        return SaleResponse.from(sale);
    }

    /**
     * Called once per CSV row by SalesCsvImportService, as a SEPARATE
     * public method (not inlined into the import loop) so each call goes
     * through Spring's transactional proxy independently - one row's
     * failure rolls back only that row's Sale+ledger insert, never the
     * rows already committed before it. This is what makes "partial
     * success" CSV import possible without wrapping the whole file in
     * one all-or-nothing transaction.
     */
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @Transactional
    public Sale recordSaleFromImport(
            UUID productId, UUID locationId, int quantity,
            BigDecimal unitPrice, Instant soldAt, UUID importBatchId, UUID userId
    ) {
        UUID tenantId = TenantContext.currentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Location location = locationRepository.findByIdAndTenantId(locationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", locationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return createSaleAndLedgerEntry(
                tenantId, product, location, quantity, unitPrice, soldAt,
                SaleSource.CSV_IMPORT, importBatchId, user);
    }

    /**
     * Every Sale, from any source, is created here and ONLY here - and
     * this is the sales module's one and only caller of
     * InventoryService.recordStockChange. The Sale insert and the ledger
     * insert happen in the same @Transactional boundary: if the ledger
     * write fails, the Sale is rolled back too. There is no code path
     * where a Sale row can exist without a matching stock movement.
     */
    private Sale createSaleAndLedgerEntry(
            UUID tenantId, Product product, Location location, int quantity,
            BigDecimal unitPrice, Instant soldAt, SaleSource source, UUID importBatchId, User user
    ) {
        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        Sale sale = new Sale(tenant, product, location, quantity, unitPrice, soldAt, source, importBatchId);
        sale = saleRepository.save(sale);

        inventoryService.recordStockChange(
                tenantId, product, location, -quantity, // negative: a sale REDUCES stock
                StockChangeReason.SALE, "SALE", sale.getId(), user);

        return sale;
    }
}

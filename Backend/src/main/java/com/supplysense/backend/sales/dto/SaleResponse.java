package com.supplysense.backend.sales.dto;

import com.supplysense.backend.sales.domain.Sale;
import com.supplysense.backend.sales.domain.SaleSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        UUID productId,
        String productSku,
        UUID locationId,
        String locationName,
        int quantity,
        BigDecimal unitPrice,
        Instant soldAt,
        SaleSource source,
        UUID importBatchId,
        Instant createdAt
) {
    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getProduct().getId(),
                sale.getProduct().getSku(),
                sale.getLocation().getId(),
                sale.getLocation().getName(),
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getSoldAt(),
                sale.getSource(),
                sale.getImportBatchId(),
                sale.getCreatedAt()
        );
    }
}

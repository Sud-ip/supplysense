package com.supplysense.backend.inventory.domain;

/**
 * PURCHASE and SALE are written by their owning modules only (purchasing -
 * not yet built; sales - M4). ADJUSTMENT/RETURN/TRANSFER are the only
 * reasons the manual adjustment endpoint is allowed to record - see
 * AdjustmentReason in the DTO package, which is the public-facing subset.
 */
public enum StockChangeReason {
    PURCHASE,
    SALE,
    ADJUSTMENT,
    RETURN,
    TRANSFER
}

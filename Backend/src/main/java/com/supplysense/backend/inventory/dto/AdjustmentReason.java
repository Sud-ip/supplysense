package com.supplysense.backend.inventory.dto;

import com.supplysense.backend.inventory.domain.StockChangeReason;

/**
 * The PUBLIC subset of StockChangeReason available through the manual
 * adjustment endpoint. PURCHASE and SALE are deliberately excluded here -
 * a caller hitting POST /inventory/adjustments cannot claim a stock
 * change was a "sale", because that would bypass the actual Sale entity
 * and its business rules (arriving in M4). This is enforced by the type
 * system: there is no AdjustmentReason.SALE to even select.
 */
public enum AdjustmentReason {
    ADJUSTMENT,
    RETURN,
    TRANSFER;

    public StockChangeReason toStockChangeReason() {
        return StockChangeReason.valueOf(this.name());
    }
}

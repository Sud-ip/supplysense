package com.supplysense.backend.alerting.dto;

public enum AlertReason {
    /** Current balance is at or below the manually-set reorder threshold. */
    BELOW_MIN_THRESHOLD,

    /**
     * The forecast-driven differentiator: even if the current balance is
     * still above the manual threshold, the moving-average forecast
     * projects a stockout within the alert window. This is exactly the
     * behavior a pure min/max threshold system CANNOT provide - a product
     * selling unusually fast can be flagged before it crosses the
     * static line.
     */
    PROJECTED_STOCKOUT_SOON
}

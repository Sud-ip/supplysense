package com.supplysense.backend.forecasting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The MVP forecasting baseline: simple moving average. Deliberately a
 * plain class with static methods and no Spring/JPA/anything else - just
 * java.math and java.time. This is intentional: the forecasting MATH
 * should be provably correct independent of how it's wired up, and
 * should be testable with plain JUnit and no database, no mocks, no
 * Spring context at all.
 *
 * Exponential smoothing (the other MVP-listed option) is not implemented
 * here yet - moving average alone satisfies the MVP requirement, and
 * adding a second method before there's a concrete reason to prefer it
 * over the first would be complexity without justification (principle
 * #15). The ForecastMethod enum already has a slot for it when needed.
 */
public final class MovingAverageForecaster {

    private MovingAverageForecaster() {
    }

    /**
     * @param totalQuantitySold total units sold across the lookback window
     * @param lookbackDays      the window size in days (must be positive)
     * @return average units sold per day, rounded to 4 decimal places
     */
    public static BigDecimal calculateDailyDemand(int totalQuantitySold, int lookbackDays) {
        if (lookbackDays <= 0) {
            throw new IllegalArgumentException("lookbackDays must be positive");
        }
        if (totalQuantitySold < 0) {
            throw new IllegalArgumentException("totalQuantitySold must not be negative");
        }
        return BigDecimal.valueOf(totalQuantitySold)
                .divide(BigDecimal.valueOf(lookbackDays), 4, RoundingMode.HALF_UP);
    }

    /**
     * @param currentQuantityOnHand current inventory balance (may be
     *                              negative - see M3's design notes on
     *                              this)
     * @param dailyDemand           predicted units sold per day
     * @param asOf                  the date to project forward from
     * @return the projected stockout date, or empty if no stockout can be
     *         meaningfully projected (zero/negative demand means the
     *         product isn't currently selling, so a "when will it run
     *         out" question has no defined answer at this demand level)
     */
    public static Optional<LocalDate> projectStockoutDate(
            int currentQuantityOnHand, BigDecimal dailyDemand, LocalDate asOf
    ) {
        if (dailyDemand.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        if (currentQuantityOnHand <= 0) {
            return Optional.of(asOf); // already out (or already negative)
        }

        BigDecimal daysUntilStockout = BigDecimal.valueOf(currentQuantityOnHand)
                .divide(dailyDemand, 0, RoundingMode.FLOOR);

        return Optional.of(asOf.plusDays(daysUntilStockout.longValue()));
    }
}

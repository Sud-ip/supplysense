package com.supplysense.backend.forecasting.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovingAverageForecasterTest {

    @Test
    void calculatesSimpleDailyAverage() {
        // 90 units sold over 30 days = 3 units/day
        BigDecimal result = MovingAverageForecaster.calculateDailyDemand(90, 30);
        assertThat(result).isEqualByComparingTo("3.0000");
    }

    @Test
    void handlesZeroSalesAsZeroDemand() {
        BigDecimal result = MovingAverageForecaster.calculateDailyDemand(0, 30);
        assertThat(result).isEqualByComparingTo("0.0000");
    }

    @Test
    void rejectsNonPositiveLookbackWindow() {
        assertThatThrownBy(() -> MovingAverageForecaster.calculateDailyDemand(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void projectsStockoutDateBasedOnCurrentBalanceAndDemand() {
        // 30 units on hand, selling 3/day -> should run out in exactly 10 days
        LocalDate today = LocalDate.of(2026, 1, 1);
        Optional<LocalDate> result = MovingAverageForecaster.projectStockoutDate(
                30, new BigDecimal("3.0000"), today);

        assertThat(result).contains(LocalDate.of(2026, 1, 11));
    }

    @Test
    void roundsDownPartialDaysConservatively() {
        // 10 units on hand, selling 3/day -> 3.33 days -> floors to 3,
        // meaning the alert fires slightly EARLY rather than late. For a
        // "will I run out" warning, erring toward an earlier warning is
        // the safer direction than erring late.
        LocalDate today = LocalDate.of(2026, 1, 1);
        Optional<LocalDate> result = MovingAverageForecaster.projectStockoutDate(
                10, new BigDecimal("3.0000"), today);

        assertThat(result).contains(LocalDate.of(2026, 1, 4));
    }

    @Test
    void returnsEmptyWhenThereIsNoMeasurableDemand() {
        Optional<LocalDate> result = MovingAverageForecaster.projectStockoutDate(
                50, BigDecimal.ZERO, LocalDate.of(2026, 1, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void treatsZeroOrNegativeBalanceAsAlreadyOutToday() {
        LocalDate today = LocalDate.of(2026, 1, 1);

        assertThat(MovingAverageForecaster.projectStockoutDate(0, new BigDecimal("2.0"), today))
                .contains(today);
        assertThat(MovingAverageForecaster.projectStockoutDate(-5, new BigDecimal("2.0"), today))
                .contains(today);
    }
}

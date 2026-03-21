package org.acme.service;

import java.util.List;

import org.acme.domain.BollingerSignal;
import org.acme.domain.TradeData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BollingerCalculatorTest {

    private static final double K = 2.0;

    @Test
    void identicalPricesCollapseBandsAndCountAsBreakoutUp() {
        // Upper == lower == last price, so the first branch (>= upper) fires before SQUEEZE.
        List<TradeData> window = List.of(
                td(100.0, 1L),
                td(100.0, 2L),
                td(100.0, 3L));

        BollingerSignal s = BollingerCalculator.calculate(window, K);

        assertEquals(100.0, s.middleBand(), 1e-9);
        assertEquals(100.0, s.upperBand(), 1e-9);
        assertEquals(100.0, s.lowerBand(), 1e-9);
        assertEquals("BREAKOUT_UP", s.signal());
    }

    @Test
    void lowVolatilityInsideBandsYieldsSqueeze() {
        // Window high is not the last tick, so the last price stays inside the bands while volatility is tiny vs mean.
        List<TradeData> window = List.of(
                td(100_000.0, 1L),
                td(100_000.0, 2L),
                td(100_000.0, 3L),
                td(100_000.3, 4L),
                td(100_000.2, 5L));

        BollingerSignal s = BollingerCalculator.calculate(window, K);

        assertEquals("SQUEEZE", s.signal());
    }

    @Test
    void breakoutUpWhenLastPriceAboveUpperBand() {
        List<TradeData> window = List.of(
                td(100.0, 1L),
                td(100.0, 2L),
                td(100.0, 3L),
                td(100.0, 4L),
                td(110.0, 5L));

        BollingerSignal s = BollingerCalculator.calculate(window, K);

        assertEquals("BREAKOUT_UP", s.signal());
        assertTrue(s.currentPrice() >= s.upperBand());
    }

    @Test
    void breakoutDownWhenLastPriceBelowLowerBand() {
        List<TradeData> window = List.of(
                td(100.0, 1L),
                td(100.0, 2L),
                td(100.0, 3L),
                td(100.0, 4L),
                td(85.0, 5L));

        BollingerSignal s = BollingerCalculator.calculate(window, K);

        assertEquals("BREAKOUT_DOWN", s.signal());
        assertTrue(s.currentPrice() <= s.lowerBand());
    }

    @Test
    void normalWhenInsideBands() {
        List<TradeData> window = List.of(
                td(98.0, 1L),
                td(99.0, 2L),
                td(100.0, 3L),
                td(101.0, 4L),
                td(100.0, 5L));

        BollingerSignal s = BollingerCalculator.calculate(window, K);

        assertEquals("NORMAL", s.signal());
        assertTrue(s.currentPrice() < s.upperBand() && s.currentPrice() > s.lowerBand());
    }

    @Test
    void bandsMatchManualTwoPassStdDev() {
        List<TradeData> window = List.of(
                td(10.0, 1L),
                td(12.0, 2L),
                td(14.0, 3L),
                td(16.0, 4L),
                td(18.0, 5L));

        double mean = window.stream().mapToDouble(TradeData::price).average().orElse(0);
        double variance = window.stream()
                .mapToDouble(t -> Math.pow(t.price() - mean, 2))
                .average()
                .orElse(0);
        double std = Math.sqrt(variance);

        BollingerSignal s = BollingerCalculator.calculate(window, K);

        assertEquals(18.0, s.currentPrice(), 1e-9);
        assertEquals(mean, s.middleBand(), 1e-9);
        assertEquals(mean + K * std, s.upperBand(), 1e-9);
        assertEquals(mean - K * std, s.lowerBand(), 1e-9);
    }

    private static TradeData td(double price, long ts) {
        return new TradeData(price, ts);
    }
}

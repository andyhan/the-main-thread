package org.acme.service;

import java.util.List;

import org.acme.domain.BollingerSignal;
import org.acme.domain.TradeData;

public final class BollingerCalculator {

    private BollingerCalculator() {
    }

    public static BollingerSignal calculate(List<TradeData> window, double k) {
        double currentPrice = window.getLast().price();

        double mean = window.stream()
                .mapToDouble(TradeData::price)
                .average()
                .orElse(0.0);

        double variance = window.stream()
                .mapToDouble(t -> Math.pow(t.price() - mean, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);

        double upper = mean + (k * stdDev);
        double lower = mean - (k * stdDev);

        String status = "NORMAL";
        if (currentPrice >= upper) {
            status = "BREAKOUT_UP";
        } else if (currentPrice <= lower) {
            status = "BREAKOUT_DOWN";
        } else if (stdDev < mean * 0.0001) {
            status = "SQUEEZE";
        }

        return new BollingerSignal(currentPrice, upper, lower, mean, status);
    }
}

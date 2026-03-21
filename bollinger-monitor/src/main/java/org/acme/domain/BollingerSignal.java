package org.acme.domain;

public record BollingerSignal(
        double currentPrice,
        double upperBand,
        double lowerBand,
        double middleBand,
        String signal) {
}
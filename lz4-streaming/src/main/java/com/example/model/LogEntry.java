package com.example.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public record LogEntry(
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
        String level,
        String service,
        String traceId,
        String message,
        Integer responseTimeMs,
        String userId) {

    public static LogEntry generateRandom() {
        return new LogEntry(
                Instant.now(),
                randomLevel(),
                "order-service",
                generateTraceId(),
                randomMessage(),
                randomResponseTime(),
                randomUserId());
    }

    private static String randomLevel() {
        double r = Math.random();
        if (r < 0.7)
            return "INFO";
        if (r < 0.9)
            return "WARN";
        if (r < 0.98)
            return "ERROR";
        return "DEBUG";
    }

    private static String generateTraceId() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    private static String randomMessage() {
        String[] messages = {
                "Order processed",
                "Cache miss",
                "Retrying payment",
                "User authenticated",
                "Circuit breaker opened",
                "Background job started"
        };
        return messages[(int) (Math.random() * messages.length)];
    }

    private static Integer randomResponseTime() {
        return (int) (Math.random() * 800);
    }

    private static String randomUserId() {
        return "user-" + (int) (Math.random() * 1000);
    }
}
package com.example.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import com.example.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LogGeneratorService {

    @Inject
    ObjectMapper mapper;

    private final AtomicLong totalLogs = new AtomicLong();

    public Multi<String> stream(int logsPerSecond) {
        long interval = 1000L / logsPerSecond;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(interval))
                .map(tick -> {
                    totalLogs.incrementAndGet();
                    try {
                        return mapper.writeValueAsString(LogEntry.generateRandom()) + "\n";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(s -> !s.isEmpty());
    }

    public String batch(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            try {
                sb.append(mapper.writeValueAsString(LogEntry.generateRandom())).append("\n");
            } catch (Exception ignored) {
            }
        }
        totalLogs.addAndGet(count);
        return sb.toString();
    }

    public long totalLogs() {
        return totalLogs.get();
    }

    public void reset() {
        totalLogs.set(0);
    }
}
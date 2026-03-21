package org.acme.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import org.acme.domain.BollingerSignal;
import org.acme.domain.TradeData;
import org.acme.ingest.BinanceClient;
import org.jspecify.annotations.NonNull;

import com.ginsberg.gatherers4j.Gatherers4j;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class BollingerService {

    @Inject
    BinanceClient binanceClient;

    private volatile MultiEmitter<? super BollingerSignal> currentEmitter;
    private volatile boolean processingStarted = false;

    private static final int WINDOW_SIZE = 20;
    private static final double K = 2.0;
    private static final @NonNull Duration DEBOUNCE_DURATION = Objects.requireNonNull(Duration.ofMillis(50));

    public Multi<BollingerSignal> stream() {
        return Multi.createFrom().emitter(emitter -> {
            this.currentEmitter = emitter;
            Log.info("New subscriber connected to stream");
            // Start processing if not already started
            if (!processingStarted) {
                synchronized (this) {
                    if (!processingStarted) {
                        processingStarted = true;
                        Executors.newSingleThreadExecutor().submit(this::processStream);
                    }
                }
            }
        });
    }

    void onStart(@Observes StartupEvent ev) {
        connectToBinance();
    }

    private void processStream() {
        Log.info("Starting stream processing - waiting for trade data...");
        try {
            java.util.stream.Stream.generate(() -> {
                try {
                    TradeData data = BinanceClient.BUFFER.take();
                    return data;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.warn("Stream processing interrupted");
                    return null;
                }
            })
                    .takeWhile(data -> data != null)
                    .gather(Gatherers4j.debounce(1, DEBOUNCE_DURATION))
                    .gather(Gatherers4j.window(WINDOW_SIZE, 1, true))
                    .map(this::calculateBollinger)
                    .forEach(signal -> {
                        MultiEmitter<? super BollingerSignal> emitter = this.currentEmitter;
                        if (emitter != null && !emitter.isCancelled()) {
                            emitter.emit(signal);
                        }
                    });
        } catch (Exception e) {
            Log.error("Error in processing stream", e);
            MultiEmitter<? super BollingerSignal> emitter = this.currentEmitter;
            if (emitter != null && !emitter.isCancelled()) {
                emitter.fail(e);
            }
        }
    }

    private BollingerSignal calculateBollinger(List<TradeData> window) {
        return BollingerCalculator.calculate(window, K);
    }

    private void connectToBinance() {
        Log.info("Attempting to connect to Binance WebSocket...");
        try {
            binanceClient.connect("wss://stream.binance.com:9443/ws/btcusdt@trade");
            // Note: connection is asynchronous, success/failure logged in BinanceClient
        } catch (Exception e) {
            Log.error("Failed to initiate Binance connection", e);
        }
    }
}
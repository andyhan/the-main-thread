package com.example.service;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.example.model.TickerData;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ApplicationScoped
@ClientEndpoint
public class BinanceWebSocketClient {

    @ConfigProperty(name = "binance.websocket.url")
    String websocketUrl;

    @ConfigProperty(name = "binance.symbols")
    List<String> symbols;

    @Inject
    TimeSeriesService timeSeriesService;

    @Inject
    ObjectMapper objectMapper;

    private Session session;
    private boolean shouldReconnect = true;
 
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    void onStart(@Observes StartupEvent ev) {
        Log.info("Starting Binance WebSocket client...");
        connectToStreams();
    }

    void onStop(@Observes ShutdownEvent ev) {
        Log.info("Shutting down Binance WebSocket client...");
        shouldReconnect = false;
        closeConnection();
    }

    private void connectToStreams() {
        try {
            // Create combined stream URL for multiple symbols
            String streams = String.join("/",
                    symbols.stream()
                            .map(s -> s + "@ticker")
                            .toList());

            String url = websocketUrl + "/" + streams;
            Log.infof("Connecting to Binance WebSocket: %s", url);

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, URI.create(url));

            Log.info("Successfully connected to Binance WebSocket");
        } catch (Exception e) {
            Log.error("Failed to connect to Binance WebSocket", e);
            scheduleReconnect();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            TickerData ticker = objectMapper.readValue(message, TickerData.class);

            Log.debugf("Received ticker for %s: price=%s, volume=%s",
                    ticker.getSymbol(),
                    ticker.getLastPrice(),
                    ticker.getVolume());

            // ReactiveRedisDataSource is non-blocking; just subscribe.
            timeSeriesService.addTick(ticker)
                    .subscribe().with(
                            ignored -> {
                            },
                            t -> Log.error("Error storing ticker in Redis TimeSeries", t));

        } catch (Exception e) {
            Log.error("Error processing WebSocket message", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        Log.infof("WebSocket connection opened: %s", session.getId());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        Log.warnf("WebSocket connection closed: %s - %s",
                closeReason.getCloseCode(),
                closeReason.getReasonPhrase());

        if (shouldReconnect) {
            scheduleReconnect();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        Log.error("WebSocket error occurred", throwable);
        if (shouldReconnect) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        Log.info("Scheduling reconnection in 5 seconds...");
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
                if (shouldReconnect) {
                    closeConnection();
                    connectToStreams();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void closeConnection() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                Log.error("Error closing WebSocket connection", e);
            }
        }
    }
}
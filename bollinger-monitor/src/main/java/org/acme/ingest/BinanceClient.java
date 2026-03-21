package org.acme.ingest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.acme.domain.TradeData;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BinanceClient {

    public static final BlockingQueue<TradeData> BUFFER = new LinkedBlockingQueue<>();

    @Inject
    Vertx vertx;

    private final ObjectMapper mapper = new ObjectMapper();
    private io.vertx.mutiny.core.http.WebSocket webSocket;

    public void connect(String uri) {
        // Check for proxy environment variables that might interfere
        String httpProxy = System.getenv("HTTP_PROXY");
        String httpsProxy = System.getenv("HTTPS_PROXY");
        if (httpProxy != null || httpsProxy != null) {
            io.quarkus.logging.Log.warn(
                    "Proxy environment variables detected - HTTP_PROXY: " + httpProxy + ", HTTPS_PROXY: " + httpsProxy);
            io.quarkus.logging.Log.warn("Using direct connection to Binance (bypassing proxy)");
        }

        WebSocketClient client = vertx.getDelegate().createWebSocketClient();

        // Parse URI to extract host, port, and path
        // Format: wss://stream.binance.com:9443/ws/btcusdt@trade
        java.net.URI parsedUri = java.net.URI.create(uri);
        String host = parsedUri.getHost();
        int port = parsedUri.getPort() != -1 ? parsedUri.getPort() : (uri.startsWith("wss://") ? 443 : 80);
        String path = parsedUri.getPath() + (parsedUri.getQuery() != null ? "?" + parsedUri.getQuery() : "");
        boolean ssl = uri.startsWith("wss://");

        // Use host/port directly to bypass proxy resolution
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(host)
                .setPort(port)
                .setURI(path)
                .setSsl(ssl);

        io.quarkus.logging.Log.info("Connecting to Binance WebSocket: " + host + ":" + port + path);

        client.connect(options)
                .onSuccess(ws -> {
                    this.webSocket = new io.vertx.mutiny.core.http.WebSocket(ws);
                    io.quarkus.logging.Log.info("Binance WebSocket connected successfully");
                    ws.textMessageHandler(message -> {
                        try {
                            TradeData data = mapper.readValue(message, TradeData.class);
                            BUFFER.offer(data);
                        } catch (Exception e) {
                            io.quarkus.logging.Log.warn("Failed to parse trade data: " + e.getMessage());
                        }
                    });
                    ws.closeHandler(v -> {
                        io.quarkus.logging.Log.warn("Binance WebSocket closed");
                    });
                })
                .onFailure(throwable -> {
                    io.quarkus.logging.Log.error("Failed to connect to Binance WebSocket", throwable);
                });
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close();
        }
    }
}
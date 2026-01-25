package com.example.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import com.example.service.BinanceWebSocketClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Liveness
@ApplicationScoped
public class BinanceStreamHealthCheck implements HealthCheck {

    @Inject
    BinanceWebSocketClient webSocketClient;

    @Override
    public HealthCheckResponse call() {

        boolean isConnected = webSocketClient != null && webSocketClient.isConnected();

        return isConnected
                ? HealthCheckResponse.up("Binance stream connection")
                : HealthCheckResponse.down("Binance stream connection");
    }
}

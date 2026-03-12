package dev.mainthread;

import java.time.Instant;

public record ShipmentStatus(
        String trackingNumber,
        ShipmentStatusCode status,
        String lastLocation,
        Instant estimatedDelivery,
        String message) {
}
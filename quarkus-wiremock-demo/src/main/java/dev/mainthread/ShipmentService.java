package dev.mainthread;

import java.time.Instant;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.mainthread.dto.CarrierTrackingResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ShipmentService {

    @Inject
    @RestClient
    CarrierClient carrierClient;

    public ShipmentStatus getStatus(String trackingNumber) {
        CarrierTrackingResponse response;
        try {
            response = carrierClient.track(trackingNumber);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 503) {
                return unavailable(trackingNumber);
            }
            throw e;
        }

        return new ShipmentStatus(
                response.trackingNumber(),
                mapStatusCode(response.statusCode()),
                extractLastLocation(response),
                parseDeliveryEstimate(response.estimatedDelivery()),
                response.statusMessage());
    }

    private ShipmentStatus unavailable(String trackingNumber) {
        return new ShipmentStatus(
                trackingNumber,
                ShipmentStatusCode.EXCEPTION,
                "unknown",
                null,
                "Carrier tracking temporarily unavailable");
    }

    private ShipmentStatusCode mapStatusCode(String carrierCode) {
        return switch (carrierCode) {
            case "LC" -> ShipmentStatusCode.LABEL_CREATED;
            case "IT" -> ShipmentStatusCode.IN_TRANSIT;
            case "OD" -> ShipmentStatusCode.OUT_FOR_DELIVERY;
            case "DL" -> ShipmentStatusCode.DELIVERED;
            default -> ShipmentStatusCode.EXCEPTION;
        };
    }

    private String extractLastLocation(CarrierTrackingResponse response) {
        if (response.events() == null || response.events().isEmpty()) {
            return "unknown";
        }
        return response.events().get(0).location();
    }

    private Instant parseDeliveryEstimate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
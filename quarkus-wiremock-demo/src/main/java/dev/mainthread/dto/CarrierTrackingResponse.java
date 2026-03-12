package dev.mainthread.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CarrierTrackingResponse(
        @JsonProperty("tracking_number") String trackingNumber,
        @JsonProperty("status_code") String statusCode,
        @JsonProperty("status_message") String statusMessage,
        @JsonProperty("events") List<CarrierEvent> events,
        @JsonProperty("estimated_delivery") String estimatedDelivery) {
}
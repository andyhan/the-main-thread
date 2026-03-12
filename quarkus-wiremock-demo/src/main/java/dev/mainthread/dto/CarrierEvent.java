package dev.mainthread.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CarrierEvent(
        String timestamp,
        String location,
        @JsonProperty("event_code") String eventCode,
        String description) {
}
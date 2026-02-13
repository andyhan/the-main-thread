package com.example.reviews.events;

import java.time.OffsetDateTime;
import java.util.Map;

public record CloudEventEnvelope(
        String specversion,
        String id,
        String source,
        String type,
        String datacontenttype,
        String time,
        Object data,
        Map<String, Object> extensions) {
    public static CloudEventEnvelope of(String id, String source, String type, Object data) {
        return new CloudEventEnvelope(
                "1.0",
                id,
                source,
                type,
                "application/json",
                OffsetDateTime.now().toString(),
                data,
                Map.of());
    }

    public CloudEventEnvelope withExtensions(Map<String, Object> ext) {
        return new CloudEventEnvelope(
                specversion, id, source, type, datacontenttype, time, data, ext);
    }
}
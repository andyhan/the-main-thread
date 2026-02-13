package com.example.reviews.events;

import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FlowInProducer {

    @Inject
    ObjectMapper mapper;

    @Inject
    @Channel("flow-in-producer")
    Emitter<byte[]> emitter;

    public void send(CloudEventEnvelope event) {
        try {
            emitter.send(mapper.writeValueAsBytes(event));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize CloudEvent", e);
        }
    }

    /**
     * Send a CloudEvent with extension attributes at the top level (e.g. XFlowInstanceId)
     * so the workflow engine can route the event to the correct instance.
     */
    public void sendWithExtensions(CloudEventEnvelope event, java.util.Map<String, Object> extensions) {
        try {
            java.util.Map<String, Object> flat = new java.util.LinkedHashMap<>();
            flat.put("specversion", event.specversion());
            flat.put("id", event.id());
            flat.put("source", event.source());
            flat.put("type", event.type());
            flat.put("datacontenttype", event.datacontenttype());
            flat.put("time", event.time());
            flat.put("data", event.data());
            if (extensions != null) {
                flat.putAll(extensions);
            }
            emitter.send(mapper.writeValueAsBytes(flat));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize CloudEvent with extensions", e);
        }
    }

    public CloudEventEnvelope newEvent(String type, Object data) {
        return CloudEventEnvelope.of(
                UUID.randomUUID().toString(),
                "/review-workflow",
                type,
                data);
    }
}
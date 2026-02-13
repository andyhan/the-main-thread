package com.example.reviews.events;

import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.example.reviews.state.ApprovalCorrelationStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FlowOutLogger {

    @Inject
    ObjectMapper mapper;

    @Inject
    ApprovalCorrelationStore store;

    @Incoming("flow-out-logger")
    public Uni<Void> onEvent(Message<byte[]> message) {
        byte[] payload = message.getPayload();
        try {
            JsonNode root = mapper.readTree(payload);
            String type = root.path("type").asText();
            JsonNode data = root.path("data");

            Log.infof("[flow-out] type=%s data=%s", type, data);

            if ("com.example.review.approval.requested".equals(type)) {
                String reviewId = data.path("reviewId").asText();

                // Instance ID: try Kafka key first (engine often uses it for correlation), then payload attributes
                String instanceId = getInstanceIdFromKafkaKey(message);
                if (instanceId == null || instanceId.isBlank()) {
                    instanceId = getInstanceIdFromPayload(root);
                }
                if (instanceId != null && !instanceId.isBlank()) {
                    store.put(reviewId, instanceId);
                    Log.infof("[correlation] reviewId=%s -> XFlowInstanceId=%s", reviewId, instanceId);
                } else {
                    Log.warnf("[flow-out] approval.requested for reviewId=%s but no instance ID (Kafka key or CE extension)", reviewId);
                }
            }
        } catch (Exception e) {
            Log.errorf("Failed to parse flow-out event: " + e.getMessage());
        }
        return Uni.createFrom().completionStage(message.ack()).replaceWithVoid();
    }

    private static String getInstanceIdFromKafkaKey(Message<byte[]> message) {
        var metaOpt = message.getMetadata(IncomingKafkaRecordMetadata.class);
        if (metaOpt.isEmpty()) return null;
        Object key = metaOpt.get().getKey();
        if (key == null) return null;
        if (key instanceof String s) return s;
        if (key instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);
        return key.toString();
    }

    private static String getInstanceIdFromPayload(JsonNode root) {
        for (String name : new String[] { "XFlowInstanceId", "xflowinstanceid", "kogitoprocinstanceid", "flowinstanceid" }) {
            String v = textOrNull(root.path(name));
            if (v != null && !v.isBlank()) return v;
        }
        if (root.has("extensions")) {
            JsonNode ext = root.path("extensions");
            for (String name : new String[] { "xflowinstanceid", "XFlowInstanceId", "kogitoprocinstanceid" }) {
                String v = textOrNull(ext.path(name));
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        String s = n.asText();
        return (s == null || s.isBlank()) ? null : s;
    }
}
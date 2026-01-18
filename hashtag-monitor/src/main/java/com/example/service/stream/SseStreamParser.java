package com.example.service.stream;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.vertx.core.buffer.Buffer;

public class SseStreamParser {

    private static final Logger LOG = Logger.getLogger(SseStreamParser.class);

    private final StringBuilder currentLine = new StringBuilder();
    private String eventType;
    private String eventData;

    public void parseChunk(Buffer buffer, Consumer<String> onUpdateEvent) {
        String chunk = buffer.toString();
        for (char c : chunk.toCharArray()) {
            if (c == '\n') {
                processLine(currentLine.toString().trim(), onUpdateEvent);
                currentLine.setLength(0);
            } else if (c != '\r') {
                currentLine.append(c);
            }
        }
    }

    private void processLine(String line, Consumer<String> onUpdateEvent) {
        if (line.isEmpty()) {
            // Empty line indicates end of event
            if (eventData != null && "update".equals(eventType)) {
                onUpdateEvent.accept(eventData);
            }
            // Reset for next event
            eventType = null;
            eventData = null;
        } else if (line.startsWith("event:")) {
            eventType = line.substring(6).trim();
            LOG.debugf("Received SSE event type: %s", eventType);
        } else if (line.startsWith("data:")) {
            eventData = line.substring(5).trim();
            LOG.debugf("Received SSE event data: %s", 
                    eventData.length() > 100 ? eventData.substring(0, 100) + "..." : eventData);
        } else if (line.startsWith("id:")) {
            // Event ID - we can ignore for now
        } else if (line.startsWith("retry:")) {
            // Retry interval - we can ignore for now
        } else if (line.startsWith(":")) {
            // Comment line - we can ignore
        }
    }
}

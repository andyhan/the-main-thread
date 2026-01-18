package com.example.service.stream;

import org.jboss.logging.Logger;

import com.example.model.MastodonStatus;
import com.example.service.buffer.MentionBuffer;
import com.example.service.matching.StatusConverter;
import com.example.service.matching.StatusMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FediBuzzStreamService {

    private static final Logger LOG = Logger.getLogger(FediBuzzStreamService.class);

    @Inject
    Vertx vertx;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MentionBuffer buffer;

    @Inject
    StatusMatcher statusMatcher;

    @Inject
    StatusConverter statusConverter;

    private volatile boolean running;

    public void start() {
        running = true;
        LOG.info("Starting FediBuzz stream service");
        LOG.infof("Connecting to stream: %s", "https://fedi.buzz/api/v1/streaming/public");

        try {
            HttpClientOptions options = new HttpClientOptions()
                    .setSsl(true)
                    .setTrustAll(true); // For development - use proper certificate validation in production
            HttpClient httpClient = vertx.createHttpClient(options);
            httpClient
                    .request(io.vertx.core.http.HttpMethod.GET, 443, "fedi.buzz", "/api/v1/streaming/public")
                    .onSuccess(request -> {
                        request.putHeader("Accept", "text/event-stream")
                                .putHeader("Cache-Control", "no-cache")
                                .putHeader("User-Agent", "hashtag-monitor/1.0")
                                .response()
                                .onSuccess(response -> {
                                    if (response.statusCode() != 200) {
                                        response.bodyHandler(body -> {
                                            LOG.errorf("Failed to connect to SSE stream: status=%d, body=%s", 
                                                    response.statusCode(), body.toString());
                                        });
                                        return;
                                    }

                                    LOG.info("Connected to SSE stream, waiting for events...");

                                    SseStreamParser parser = new SseStreamParser();
                                    // Capture references to avoid CDI context issues in Vert.x callbacks
                                    final StatusMatcher matcher = this.statusMatcher;
                                    final StatusConverter converter = this.statusConverter;
                                    final MentionBuffer buff = this.buffer;
                                    
                                    response.handler(buffer -> {
                                        parser.parseChunk(buffer, eventData -> {
                                            // Process on worker thread to ensure CDI context is available
                                            vertx.executeBlocking(() -> {
                                                processEvent(eventData, matcher, converter, buff);
                                                return null;
                                            })
                                            .onFailure(ex -> LOG.warnf(ex, "Error processing SSE event"));
                                        });
                                    });

                                    response.exceptionHandler(ex -> {
                                        if (running) {
                                            LOG.warnf(ex, "Error in SSE stream connection");
                                        }
                                    });

                                    response.endHandler(v -> {
                                        if (running) {
                                            LOG.warn("SSE stream connection closed");
                                        }
                                    });
                                })
                                .onFailure(ex -> {
                                    if (running) {
                                        LOG.errorf(ex, "Failed to get SSE stream response");
                                    }
                                });
                        
                        request.end();
                    })
                    .onFailure(ex -> {
                        if (running) {
                            LOG.errorf(ex, "Failed to create SSE stream request");
                        }
                    });

            LOG.info("SSE request initiated");

        } catch (Exception e) {
            LOG.errorf(e, "Failed to start stream: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    private void processEvent(String eventData, StatusMatcher matcher, StatusConverter converter, MentionBuffer buff) {
        if (!running) {
            return;
        }

        try {
            MastodonStatus status = objectMapper.readValue(eventData, MastodonStatus.class);
            
            // Validate status has required fields
            if (status == null || status.id() == null || status.id().isBlank()) {
                LOG.debugf("Skipping status with null or blank id");
                return;
            }
            
            LOG.debugf("Parsed status: id=%s, tags=%s", status.id(), 
                    status.tags() != null ? status.tags().stream().map(t -> t.name()).toList() : "null");
            
            if (matcher.matches(status)) {
                var result = converter.toEntities(status);
                for (var mention : result.mentions()) {
                    if (mention != null) {
                        buff.add(result.status(), mention);
                        LOG.debugf("Added mention to buffer: statusId=%s, hashtag=%s, bufferSize=%d", 
                                result.status().statusId, mention.hashtag, buff.size());
                    }
                }
            } else {
                LOG.debugf("Status does not match hashtags: id=%s", status.id());
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to parse SSE event data: %s", e.getMessage());
        }
    }
}

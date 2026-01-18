package com.example.lifecycle;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.example.service.stream.FediBuzzStreamService;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

@ApplicationScoped
public class StreamStartup {

    private static final Logger LOG = Logger.getLogger(StreamStartup.class);

    @Inject
    FediBuzzStreamService streamService;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "app.stream.auto-start", defaultValue = "true")
    boolean autoStart;

    void onStart(@Observes StartupEvent event) {
        // Configure Vert.x exception handler to catch ProcessingException from SSE events
        vertx.exceptionHandler(throwable -> {
            if (throwable instanceof ProcessingException) {
                // Some SSE events may not have data or have null media type
                // This is normal for keep-alive messages or other non-data events
                LOG.debugf(throwable, "SSE processing exception (this is normal for some events)");
            } else {
                LOG.error("Uncaught exception in Vert.x", throwable);
            }
        });

        if (autoStart) {
            streamService.start();
        }
    }
}

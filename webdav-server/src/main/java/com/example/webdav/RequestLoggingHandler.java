package com.example.webdav;

import java.util.logging.Logger;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class RequestLoggingHandler {

    private static final Logger LOG = Logger.getLogger(RequestLoggingHandler.class.getName());

    void init(@Observes Router router) {

        // Add logging handler for all routes
        router.route().handler(this::logRequest);
        LOG.info("Request logging handler registered");
    }

    void logRequest(RoutingContext context) {
        LOG.info(() -> String.format("Incoming request: %s %s from %s",
                context.request().method(),
                context.request().uri(),
                context.request().remoteAddress()));
        LOG.fine(() -> "Request headers: " + context.request().headers().entries());
        LOG.fine(() -> "Request path: " + context.normalizedPath());

        // Continue to next handler
        context.next();
    }
}
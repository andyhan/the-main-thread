package com.example.webdav;

import java.nio.file.Path;
import java.util.logging.Logger;

import io.quarkus.vertx.web.Route;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetHandler {

    private static final Logger LOG = Logger.getLogger(GetHandler.class.getName());

    @Inject
    Vertx vertx;

    @Route(methods = { Route.HttpMethod.GET, Route.HttpMethod.HEAD }, path = "/*")
    void get(RoutingContext context) {
        String root = context.vertx().getOrCreateContext().config().getString("webdav.root", "./data");
        String path = context.normalizedPath();

        LOG.info(() -> String.format("%s request: %s (normalized: %s) from %s",
                context.request().method(),
                context.request().uri(),
                path,
                context.request().remoteAddress()));

        Path filePath = Path.of(root, path);

        vertx.fileSystem().exists(filePath.toString(), exists -> {
            if (exists.failed() || !exists.result()) {
                context.response().setStatusCode(404).end();
                return;
            }

            vertx.fileSystem().props(filePath.toString(), props -> {
                if (props.failed()) {
                    context.response().setStatusCode(500).end();
                    return;
                }

                context.response()
                        .putHeader("Content-Length", String.valueOf(props.result().size()))
                        .putHeader("Last-Modified", String.valueOf(props.result().lastModifiedTime()));

                if (context.request().method().name().equals("HEAD")) {
                    context.response().end();
                } else {
                    context.response().sendFile(filePath.toString());
                }
            });
        });
    }
}
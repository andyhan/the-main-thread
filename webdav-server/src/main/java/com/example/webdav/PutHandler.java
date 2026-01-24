package com.example.webdav;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PutHandler {

    private static final Logger LOG = Logger.getLogger(PutHandler.class.getName());

    @Inject
    Vertx vertx;

    void init(@Observes Router router) {
        // Configure BodyHandler specifically for PUT routes, then our handler
        router.route(HttpMethod.PUT, "/*")
                .handler(BodyHandler.create().setBodyLimit(-1))
                .handler(this::put);
        LOG.info("PUT route registered with BodyHandler");
    }

    void put(RoutingContext context) {
        String root = context.vertx().getOrCreateContext().config().getString("webdav.root", "./data");
        String normalizedPath = context.normalizedPath();
        Path rootDir = Path.of(root).toAbsolutePath().normalize();
        Path target = normalizedPath.equals("/") ? rootDir : rootDir.resolve(normalizedPath.substring(1));

        // Check if this is a macOS metadata file we should ignore
        String fileName = target.getFileName() != null ? target.getFileName().toString() : "";
        if (fileName.startsWith("._") || fileName.equals(".DS_Store") ||
                fileName.startsWith(".metadata_") || fileName.equals(".Spotlight-V100") ||
                fileName.equals(".hidden") || fileName.equals(".metadata_never_index") ||
                fileName.equals(".metadata_never_index_unless_rootfs") ||
                fileName.equals(".metadata_direct_scope_only")) {
            LOG.fine(() -> String.format("Ignoring macOS metadata file: %s", target));
            context.response().setStatusCode(204).end();
            return;
        }

        LOG.info(() -> String.format("PUT request: %s (target: %s) from %s",
                context.request().uri(),
                target,
                context.request().remoteAddress()));

        // BodyHandler should have read the body - get it from context
        // Note: macOS Finder sends PUT with Content-Length: 0 to create empty files
        // first, then follows up with actual content. We must allow empty bodies.
        var body = context.body();
        final io.vertx.core.buffer.Buffer bodyBuffer;

        if (body == null || body.buffer() == null) {
            // Empty body is valid - create an empty file
            LOG.fine(() -> String.format("PUT request with empty body for %s (creating empty file)", target));
            bodyBuffer = io.vertx.core.buffer.Buffer.buffer();
        } else {
            bodyBuffer = body.buffer();
            LOG.fine(() -> String.format("Body from context.body(): %d bytes", bodyBuffer.length()));
        }

        processPutRequest(context, target, bodyBuffer);
    }

    private void processPutRequest(RoutingContext context, Path target, io.vertx.core.buffer.Buffer bodyBuffer) {
        LOG.info(() -> String.format("Processing PUT: %s (%d bytes%s)",
                target, bodyBuffer.length(), bodyBuffer.length() == 0 ? " - creating empty file" : ""));

        // Create parent directories and write file
        vertx.executeBlocking(() -> {
            Files.createDirectories(target.getParent());
            return bodyBuffer;
        }).onFailure(e -> {
            LOG.warning(() -> String.format("PUT failed for %s: %s", target, e.getMessage()));
            context.response().setStatusCode(500).end();
        }).onSuccess(buffer -> {
            vertx.fileSystem().writeFile(
                    target.toString(),
                    buffer,
                    res -> {
                        if (res.succeeded()) {
                            LOG.info(() -> String.format("PUT success for %s", target));
                            context.response().setStatusCode(201).end();
                        } else {
                            LOG.warning(() -> String.format("PUT write failed for %s: %s", target,
                                    res.cause().getMessage()));
                            context.response().setStatusCode(500).end();
                        }
                    });
        });
    }
}
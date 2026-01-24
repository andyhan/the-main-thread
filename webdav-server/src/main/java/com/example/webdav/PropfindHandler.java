package com.example.webdav;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PropfindHandler {

    private static final Logger LOG = Logger.getLogger(PropfindHandler.class.getName());

    @Inject
    Vertx vertx;

    void init(@Observes Router router) {
        router.route(HttpMethod.PROPFIND, "/*").handler(this::propfind);
        LOG.info("PROPFIND route registered");
    }

    void propfind(RoutingContext context) {
        String root = context.vertx().getOrCreateContext().config().getString("webdav.root", "./data");
        String normalizedPath = context.normalizedPath();
        Path rootDir = Path.of(root).toAbsolutePath().normalize();

        // Handle root path - if path is "/", target is rootDir, otherwise build path
        Path target = normalizedPath.equals("/") ? rootDir : rootDir.resolve(normalizedPath.substring(1));

        LOG.info(() -> String.format("PROPFIND request: %s (root: %s, target: %s, absolute: %s) from %s",
                context.request().uri(),
                root,
                target,
                target.toAbsolutePath(),
                context.request().remoteAddress()));
        LOG.fine(() -> "Request headers: " + context.request().headers().entries());

        String depth = context.request().getHeader("Depth");

        vertx.executeBlocking(() -> {
            // Ensure root directory exists
            if (!Files.exists(rootDir)) {
                Files.createDirectories(rootDir);
                LOG.info(() -> String.format("Created root directory: %s (absolute: %s)", rootDir,
                        rootDir.toAbsolutePath()));
            } else {
                LOG.fine(() -> String.format("Root directory exists: %s (absolute: %s)", rootDir,
                        rootDir.toAbsolutePath()));
            }

            if (!Files.exists(target)) {
                LOG.warning(() -> String.format("Target does not exist: %s (absolute: %s)", target,
                        target.toAbsolutePath()));
                throw new RuntimeException("Not found");
            }

            // Build response for the requested resource itself
            final String href;
            if (normalizedPath.equals("/")) {
                href = "/";
            } else {
                href = normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
            }

            boolean isDirectory = Files.isDirectory(target);
            String resourceType = isDirectory ? "<d:collection/>" : "";
            long contentLength = isDirectory ? 0 : Files.size(target);
            FileTime lastModifiedTime = Files.getLastModifiedTime(target);
            // Format date in RFC 822 format (required by WebDAV)
            String lastModified = DateTimeFormatter.RFC_1123_DATE_TIME
                    .withZone(ZoneId.of("GMT"))
                    .format(Instant.ofEpochMilli(lastModifiedTime.toMillis()));

            // Generate ETag (simple implementation)
            String etag = "\"" + lastModifiedTime.toMillis() + "-" + contentLength + "\"";

            String selfResponse = String.format(
                    "<d:response>" +
                            "<d:href>%s</d:href>" +
                            "<d:propstat>" +
                            "<d:prop>" +
                            "<d:displayname>%s</d:displayname>" +
                            "<d:resourcetype>%s</d:resourcetype>" +
                            "<d:getcontentlength>%d</d:getcontentlength>" +
                            "<d:getlastmodified>%s</d:getlastmodified>" +
                            "<d:getetag>%s</d:getetag>" +
                            "<d:creationdate>%s</d:creationdate>" +
                            "</d:prop>" +
                            "<d:status>HTTP/1.1 200 OK</d:status>" +
                            "</d:propstat>" +
                            "</d:response>",
                    href,
                    target.getFileName().toString().isEmpty() ? "/" : target.getFileName().toString(),
                    resourceType,
                    contentLength,
                    lastModified,
                    etag,
                    lastModified // Use lastModified as creationdate for simplicity
            );

            // If Depth=1 or Infinity, also include children
            final String childrenResponse;
            if (!"0".equals(depth) && isDirectory) {
                LOG.fine(() -> String.format("Depth=%s, listing children of %s", depth, target));
                try {
                    String children = Files.list(target)
                            .map(p -> {
                                try {
                                    boolean childIsDir = Files.isDirectory(p);
                                    String childHref = href + p.getFileName().toString() + (childIsDir ? "/" : "");
                                    String childResourceType = childIsDir ? "<d:collection/>" : "";
                                    long childContentLength = childIsDir ? 0 : Files.size(p);
                                    FileTime childLastModifiedTime = Files.getLastModifiedTime(p);
                                    String childLastModified = DateTimeFormatter.RFC_1123_DATE_TIME
                                            .withZone(ZoneId.of("GMT"))
                                            .format(Instant.ofEpochMilli(childLastModifiedTime.toMillis()));
                                    String childEtag = "\"" + childLastModifiedTime.toMillis() + "-"
                                            + childContentLength + "\"";

                                    return String.format(
                                            "<d:response>" +
                                                    "<d:href>%s</d:href>" +
                                                    "<d:propstat>" +
                                                    "<d:prop>" +
                                                    "<d:displayname>%s</d:displayname>" +
                                                    "<d:resourcetype>%s</d:resourcetype>" +
                                                    "<d:getcontentlength>%d</d:getcontentlength>" +
                                                    "<d:getlastmodified>%s</d:getlastmodified>" +
                                                    "<d:getetag>%s</d:getetag>" +
                                                    "<d:creationdate>%s</d:creationdate>" +
                                                    "</d:prop>" +
                                                    "<d:status>HTTP/1.1 200 OK</d:status>" +
                                                    "</d:propstat>" +
                                                    "</d:response>",
                                            childHref,
                                            p.getFileName().toString(),
                                            childResourceType,
                                            childContentLength,
                                            childLastModified,
                                            childEtag,
                                            childLastModified);
                                } catch (java.io.IOException e) {
                                    throw new RuntimeException("Failed to process file: " + p, e);
                                }
                            })
                            .collect(Collectors.joining());
                    childrenResponse = children;
                    LOG.fine(() -> String.format("Found children, response length: %d", childrenResponse.length()));
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Failed to list directory: " + e.getMessage(), e);
                }
            } else {
                LOG.fine(() -> String.format("Depth=%s, isDirectory=%s, skipping children", depth, isDirectory));
                childrenResponse = "";
            }

            return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<d:multistatus xmlns:d=\"DAV:\">" +
                    selfResponse +
                    childrenResponse +
                    "</d:multistatus>";
        }).onFailure(e -> {
            LOG.warning(() -> String.format("PROPFIND failed for %s: %s", target, e.getMessage()));
            context.response().setStatusCode(404).end();
        }).onSuccess(xml -> {
            LOG.info(() -> String.format("PROPFIND success for %s, returning %d bytes", target,
                    xml.toString().length()));
            context.response()
                    .putHeader("Content-Type", "application/xml; charset=utf-8")
                    .setStatusCode(207)
                    .end(xml.toString());
        });
    }
}
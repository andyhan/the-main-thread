package com.example.webdav;

import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.vertx.web.Route;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DeleteAndMkcolHandler {

    @Inject
    Vertx vertx;

    @Route(methods = Route.HttpMethod.DELETE, path = "/*")
    void delete(RoutingContext context) {
        String root = context.vertx().getOrCreateContext().config().getString("webdav.root", "./data");
        Path target = Path.of(root, context.normalizedPath());

        vertx.executeBlocking(() -> {
            if (Files.exists(target)) {
                if (Files.isDirectory(target)) {
                    Files.walk(target)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                } else {
                    Files.delete(target);
                }
            }
            return null;
        }).onSuccess(v -> {
            context.response().setStatusCode(204).end();
        }).onFailure(e -> {
            context.response().setStatusCode(404).end();
        });
    }

    void init(@Observes Router router) {
        router.route(HttpMethod.MKCOL, "/*").handler(this::mkcol);
    }

    void mkcol(RoutingContext context) {
        String root = context.vertx().getOrCreateContext().config().getString("webdav.root", "./data");
        Path target = Path.of(root, context.normalizedPath());

        vertx.executeBlocking(() -> {
            Files.createDirectories(target);
            return null;
        }).onSuccess(v -> {
            context.response().setStatusCode(201).end();
        }).onFailure(e -> {
            context.response().setStatusCode(405).end();
        });
    }
}
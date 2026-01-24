package com.example.webdav;

import java.util.logging.Logger;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OptionsHandler {

    private static final Logger LOG = Logger.getLogger(OptionsHandler.class.getName());

    @Route(methods = Route.HttpMethod.OPTIONS, path = "/*")
    void options(RoutingContext context) {
        LOG.info(() -> String.format("OPTIONS request: %s %s from %s", 
                context.request().method(), 
                context.request().uri(),
                context.request().remoteAddress()));
        LOG.fine(() -> "Request headers: " + context.request().headers().entries());
        
        context.response()
                .putHeader("DAV", "1,2")
                .putHeader("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, PROPFIND, COPY, MOVE, LOCK, UNLOCK")
                .putHeader("MS-Author-Via", "DAV")
                .setStatusCode(200)
                .end();
        
        LOG.info("OPTIONS response sent with status 200");
    }
}
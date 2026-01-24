package com.example.webdav;

import java.util.UUID;
import java.util.logging.Logger;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class LockHandler {

        private static final Logger LOG = Logger.getLogger(LockHandler.class.getName());

        void init(@Observes Router router) {
                router.route(HttpMethod.valueOf("LOCK"), "/*").handler(this::lock);
                router.route(HttpMethod.valueOf("UNLOCK"), "/*").handler(this::unlock);
                LOG.info("LOCK/UNLOCK routes registered");
        }

        void lock(RoutingContext context) {
                String normalizedPath = context.normalizedPath();
                String lockToken = "opaquelocktoken:" + UUID.randomUUID().toString();

                LOG.info(() -> String.format("LOCK request: %s from %s, returning token: %s",
                                normalizedPath,
                                context.request().remoteAddress(),
                                lockToken));

                // Return a WebDAV lock response
                String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<d:prop xmlns:d=\"DAV:\">\n" +
                                "  <d:lockdiscovery>\n" +
                                "    <d:activelock>\n" +
                                "      <d:locktype><d:write/></d:locktype>\n" +
                                "      <d:lockscope><d:exclusive/></d:lockscope>\n" +
                                "      <d:depth>infinity</d:depth>\n" +
                                "      <d:owner>\n" +
                                "        <d:href>anonymous</d:href>\n" +
                                "      </d:owner>\n" +
                                "      <d:timeout>Second-3600</d:timeout>\n" +
                                "      <d:locktoken>\n" +
                                "        <d:href>" + lockToken + "</d:href>\n" +
                                "      </d:locktoken>\n" +
                                "      <d:lockroot>\n" +
                                "        <d:href>" + normalizedPath + "</d:href>\n" +
                                "      </d:lockroot>\n" +
                                "    </d:activelock>\n" +
                                "  </d:lockdiscovery>\n" +
                                "</d:prop>";

                context.response()
                                .putHeader("Content-Type", "application/xml; charset=utf-8")
                                .putHeader("Lock-Token", "<" + lockToken + ">")
                                .setStatusCode(200)
                                .end(xml);
        }

        void unlock(RoutingContext context) {
                String normalizedPath = context.normalizedPath();
                String lockToken = context.request().getHeader("Lock-Token");

                LOG.info(() -> String.format("UNLOCK request: %s (token: %s) from %s",
                                normalizedPath,
                                lockToken,
                                context.request().remoteAddress()));

                // Always succeed - we don't actually track locks
                context.response()
                                .setStatusCode(204)
                                .end();
        }
}
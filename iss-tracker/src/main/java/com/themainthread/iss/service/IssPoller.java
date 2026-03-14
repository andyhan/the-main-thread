package com.themainthread.iss.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.themainthread.iss.client.IssApiClient;
import com.themainthread.iss.service.IssPositionCache.PositionFix;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IssPoller {

    private static final Logger LOG = Logger.getLogger(IssPoller.class);

    @Inject
    @RestClient
    IssApiClient client;

    @Inject
    IssPositionCache cache;

    @Scheduled(every = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            var response = client.fetchPosition();

            if (!"success".equalsIgnoreCase(response.message())) {
                LOG.warnf("Unexpected upstream response message: %s", response.message());
                return;
            }

            PositionFix fix = cache.update(response);
            cache.broadcast(fix);

            LOG.debugf("ISS fix updated - lat=%.4f lon=%.4f x=%d y=%d",
                    fix.latitude(), fix.longitude(), fix.pixelX(), fix.pixelY());

        } catch (Exception e) {
            LOG.errorf("Failed to fetch ISS position: %s", e.getMessage());
        }
    }
}
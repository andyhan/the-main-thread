package com.example.auth;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TokenCleanupJob {

    private static final Logger LOG = Logger.getLogger(TokenCleanupJob.class);

    @Scheduled(every = "1h")
    @Transactional
    void purge() {
        long deleted = AuthToken.deleteExpiredOrUsed();
        LOG.infof("Deleted %d used or expired auth tokens", deleted);
    }
}
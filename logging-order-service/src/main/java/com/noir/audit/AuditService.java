package com.noir.audit;

import org.jboss.logging.Logger;

import io.quarkus.arc.log.LoggerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuditService {

    @Inject
    @LoggerName("com.noir.audit")
    Logger auditLogger;

    public void auditOrder(String orderId, String userId) {
        auditLogger.infof("AUDIT orderId=%s userId=%s",
                orderId, userId);
    }
}
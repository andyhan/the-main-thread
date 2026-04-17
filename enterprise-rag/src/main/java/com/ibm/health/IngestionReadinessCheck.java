package com.ibm.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import com.ibm.ingest.IndexingState;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Readiness stays {@code DOWN} until background document ingestion and embedding complete.
 */
@Readiness
@ApplicationScoped
public class IngestionReadinessCheck implements HealthCheck {

    @Inject
    IndexingState indexingState;

    @Override
    public HealthCheckResponse call() {
        if (indexingState.isIndexReady()) {
            return HealthCheckResponse.up("ingestion");
        }
        return HealthCheckResponse.down("ingestion");
    }
}

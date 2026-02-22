package dev.example.catalog;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class StorageReadiness implements HealthCheck {

    @Inject
    CatalogConfig config;

    @Override
    public HealthCheckResponse call() {
        boolean storageOk = "in-memory".equals(config.getStorageBackend());

        return HealthCheckResponse.named("catalog-storage")
                .status(storageOk)
                .withData("backend", config.getStorageBackend())
                .build();
    }
}
package dev.example.catalog;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;

@Liveness
@ApplicationScoped
public class CatalogLiveness implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("catalog-liveness")
                .status(true)
                .build();
    }
}

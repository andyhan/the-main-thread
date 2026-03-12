package com.mainthread.k8s;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class InformerHealthCheck implements HealthCheck {

    @Inject
    ClusterInformerRegistry registry;

    @Override
    public HealthCheckResponse call() {
        boolean podsRunning = registry.pods().isRunning();
        boolean podsWatching = registry.pods().isWatching();
        boolean podsSynced = registry.pods().hasSynced();

        boolean deploymentsRunning = registry.deployments().isRunning();
        boolean deploymentsWatching = registry.deployments().isWatching();
        boolean deploymentsSynced = registry.deployments().hasSynced();

        boolean healthy = podsWatching && deploymentsWatching;

        return HealthCheckResponse.named("kubernetes-informers")
                .status(healthy)
                .withData("pods.isRunning", podsRunning)
                .withData("pods.isWatching", podsWatching)
                .withData("pods.hasSynced", podsSynced)
                .withData("deployments.isRunning", deploymentsRunning)
                .withData("deployments.isWatching", deploymentsWatching)
                .withData("deployments.hasSynced", deploymentsSynced)
                .build();
    }
}
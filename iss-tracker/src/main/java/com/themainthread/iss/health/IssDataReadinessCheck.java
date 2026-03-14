package com.themainthread.iss.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import com.themainthread.iss.service.IssPositionCache;
import com.themainthread.iss.service.IssPositionCache.PositionFix;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class IssDataReadinessCheck implements HealthCheck {

    @Inject
    IssPositionCache cache;

    @Override
    public HealthCheckResponse call() {
        if (!cache.hasData()) {
            return HealthCheckResponse.named("iss-data")
                    .down()
                    .withData("reason", "No successful poll yet")
                    .build();
        }

        PositionFix fix = cache.latest();

        return HealthCheckResponse.named("iss-data")
                .up()
                .withData("latitude", String.valueOf(fix.latitude()))
                .withData("longitude", String.valueOf(fix.longitude()))
                .withData("updatedAt", fix.updatedAt().toString())
                .build();
    }
}
package com.example.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import com.example.entity.HashtagMention;
import com.example.service.buffer.MentionBuffer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class StreamHealthCheck implements HealthCheck {

    @Inject
    MentionBuffer buffer;

    @Override
    public HealthCheckResponse call() {
        if (buffer == null) {
            return HealthCheckResponse.named("fedi-stream")
                    .withData("error", "Buffer is null")
                    .down()
                    .build();
        }
        
        int bufferSize = buffer.size();
        long totalRecords = getTotalRecordCount();
        
        return HealthCheckResponse.named("fedi-stream")
                .withData("bufferSize", bufferSize)
                .withData("totalRecords", totalRecords)
                .up()
                .build();
    }

    private long getTotalRecordCount() {
        try {
            return HashtagMention.count();
        } catch (Exception e) {
            return -1L; // Indicate error
        }
    }
}

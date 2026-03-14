package com.themainthread.iss.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.themainthread.iss.client.IssNowResponse;
import com.themainthread.iss.util.MercatorProjection;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IssPositionCache {

    private static final int PATH_HISTORY_SIZE = 60; // ~10 min at 10s polls

    public record PositionFix(
            double latitude,
            double longitude,
            int pixelX,
            int pixelY,
            long timestamp,
            Instant updatedAt) {
    }

    private final AtomicReference<PositionFix> latest = new AtomicReference<>(null);
    private final List<PositionFix> pathHistory = Collections.synchronizedList(new ArrayList<>());
    private final BroadcastProcessor<PositionFix> processor = BroadcastProcessor.create();

    public PositionFix update(IssNowResponse response) {
        double lat = response.issPosition().latDouble();
        double lon = response.issPosition().lonDouble();
        int[] pixels = MercatorProjection.toPixel(lat, lon);

        PositionFix fix = new PositionFix(
                lat,
                lon,
                pixels[0],
                pixels[1],
                response.timestamp(),
                Instant.now());

        latest.set(fix);
        synchronized (pathHistory) {
            pathHistory.add(fix);
            if (pathHistory.size() > PATH_HISTORY_SIZE) {
                pathHistory.remove(0);
            }
        }
        return fix;
    }

    public List<PositionFix> path() {
        synchronized (pathHistory) {
            return List.copyOf(pathHistory);
        }
    }

    public PositionFix latest() {
        return latest.get();
    }

    public boolean hasData() {
        return latest.get() != null;
    }

    public void broadcast(PositionFix fix) {
        processor.onNext(fix);
    }

    public Multi<PositionFix> stream() {
        return processor;
    }
}
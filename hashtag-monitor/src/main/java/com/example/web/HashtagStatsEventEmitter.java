package com.example.web;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.example.entity.HashtagMention;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

@ApplicationScoped
public class HashtagStatsEventEmitter {

    private static final Logger LOG = Logger.getLogger(HashtagStatsEventEmitter.class);

    @Inject
    Sse sse;

    public Sse getSse() {
        return sse;
    }

    @Inject
    ObjectMapper objectMapper;

    private SseBroadcaster broadcaster;
    private volatile boolean needsUpdate = false;

    public synchronized SseBroadcaster getBroadcaster() {
        if (broadcaster == null) {
            broadcaster = sse.newBroadcaster();
            broadcaster.onClose(sink -> LOG.debugf("SSE client disconnected"));
            broadcaster.onError((sink, throwable) -> LOG.warnf(throwable, "SSE error"));
        }
        return broadcaster;
    }

    /**
     * Emit updated statistics to all connected clients
     */
    public void emitUpdate() {
        needsUpdate = true;
    }

    /**
     * Periodically check if update is needed and broadcast stats
     */
    @Scheduled(every = "2s")
    void broadcastUpdate() {
        if (!needsUpdate) {
            return;
        }

        try {
            List<HashtagStats> stats = getHashtagStats();
            long totalHashtags = getTotalHashtagCount();
            long totalMentions = stats.stream().mapToLong(HashtagStats::count).sum();
            
            // Create response object with stats and totals
            StatsResponse response = new StatsResponse(stats, totalHashtags, totalMentions);
            String statsJson = objectMapper.writeValueAsString(response);
            
            SseBroadcaster bc = getBroadcaster();
            if (bc != null) {
                bc.broadcast(sse.newEvent("stats", statsJson));
                needsUpdate = false;
                LOG.debugf("Broadcasted stats update to %d clients", getClientCount());
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to broadcast stats update");
        }
    }

    private List<HashtagStats> getHashtagStats() {
        // Get top 50 hashtags with their counts
        List<Object[]> results = HashtagMention.getEntityManager()
                .createQuery("SELECT h.hashtag, COUNT(h) FROM HashtagMention h GROUP BY h.hashtag ORDER BY COUNT(h) DESC", Object[].class)
                .setMaxResults(50)
                .getResultList();

        return results.stream()
                .map(row -> new HashtagStats((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private long getTotalHashtagCount() {
        // Get total count of unique hashtags (not limited to 50)
        Long count = HashtagMention.getEntityManager()
                .createQuery("SELECT COUNT(DISTINCT h.hashtag) FROM HashtagMention h", Long.class)
                .getSingleResult();
        return count != null ? count : 0L;
    }

    public record StatsResponse(List<HashtagStats> stats, long totalHashtags, long totalMentions) {
    }

    public int getClientCount() {
        // Note: SseBroadcaster doesn't expose client count directly
        // This is a simplified implementation
        return broadcaster != null ? 1 : 0; // Approximation
    }

    public record HashtagStats(String hashtag, long count) {
    }
}

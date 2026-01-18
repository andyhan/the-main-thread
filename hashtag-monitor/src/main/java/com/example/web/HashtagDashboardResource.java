package com.example.web;

import java.util.List;
import java.util.stream.Collectors;

import com.example.entity.HashtagMention;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/dashboard")
public class HashtagDashboardResource {

    @Inject
    Template dashboard;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    HashtagStatsEventEmitter eventEmitter;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        List<HashtagStats> stats = getHashtagStats();
        long totalMentions = stats.stream().mapToLong(HashtagStats::count).sum();
        long totalHashtags = getTotalHashtagCount();
        
        // Convert to JSON string for JavaScript using ObjectMapper for proper escaping
        String statsJson;
        try {
            statsJson = objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            statsJson = "[]";
        }
        
        return dashboard.data("stats", stats)
                .data("totalHashtags", totalHashtags)
                .data("totalMentions", totalMentions)
                .data("topHashtag", stats.isEmpty() ? null : stats.get(0).hashtag())
                .data("statsJson", statsJson);
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

    @GET
    @Path("/stats/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamStats(SseEventSink eventSink) {
        eventEmitter.getBroadcaster().register(eventSink);
        // Send initial data immediately
        try {
            List<HashtagStats> stats = getHashtagStats();
            long totalHashtags = getTotalHashtagCount();
            long totalMentions = stats.stream().mapToLong(HashtagStats::count).sum();
            
            // Create response object with stats and totals
            StatsResponse response = new StatsResponse(stats, totalHashtags, totalMentions);
            String statsJson = objectMapper.writeValueAsString(response);
            eventSink.send(eventEmitter.getSse().newEvent("stats", statsJson));
        } catch (Exception e) {
            // Log but don't fail - client will get updates via broadcaster
        }
    }

    public record StatsResponse(List<HashtagStats> stats, long totalHashtags, long totalMentions) {
    }

    public record HashtagStats(String hashtag, long count) {
    }
}

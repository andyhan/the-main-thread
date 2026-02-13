package com.example.reviews.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApprovalCorrelationStore {

    private final Map<String, String> reviewToInstance = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> reviewToState = new ConcurrentHashMap<>();

    public void put(String reviewId, String instanceId) {
        reviewToInstance.put(reviewId, instanceId);
    }

    public String getInstanceId(String reviewId) {
        return reviewToInstance.get(reviewId);
    }

    /** Stash workflow state by reviewId so it can be restored after the approval event (which replaces state). */
    public void putState(String reviewId, Map<String, Object> state) {
        reviewToState.put(reviewId, state);
    }

    /** Restore workflow state for a reviewId (and remove from stash). */
    public Map<String, Object> getState(String reviewId) {
        return reviewToState.remove(reviewId);
    }
}
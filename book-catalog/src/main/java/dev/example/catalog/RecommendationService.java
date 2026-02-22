package dev.example.catalog;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecommendationService {

    @Inject
    RecommendationClient client;

    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "defaultRecommendations")
    public List<String> getRecommendations(String bookId) {
        return client.fetchRecommendations(bookId);
    }

    public List<String> defaultRecommendations(String bookId) {
        return List.of("Popular-Book-1", "Popular-Book-2");
    }
}
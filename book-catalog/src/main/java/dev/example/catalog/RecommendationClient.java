package dev.example.catalog;

import java.util.List;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationClient {

    private final Random random = new Random();

    public List<String> fetchRecommendations(String bookId) {
        if (random.nextInt(3) == 0) {
            throw new RuntimeException("Recommendation engine unavailable");
        }

        if (random.nextInt(5) == 0) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return List.of("Book-A", "Book-B", "Book-C");
    }
}
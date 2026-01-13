package com.example.metrics;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SemanticSimilarityCalculator {

    @Inject
    EmbeddingModel embeddingModel;

    /**
     * Calculate cosine similarity between two texts using embeddings
     */
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null ||
                text1.trim().isEmpty() || text2.trim().isEmpty()) {
            return 0.0;
        }

        try {
            Embedding embedding1 = embeddingModel.embed(text1).content();
            Embedding embedding2 = embeddingModel.embed(text2).content();

            return cosineSimilarity(embedding1.vector(), embedding2.vector());
        } catch (Exception e) {
            System.err.println("Error calculating semantic similarity: " + e.getMessage());
            return 0.0;
        }
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have same dimensions");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
package com.example.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BleuCalculator {

    /**
     * Calculate BLEU score with n-gram precision up to 4-grams
     */
    public double calculateBleu(String candidate, String reference) {
        if (candidate == null || reference == null ||
                candidate.trim().isEmpty() || reference.trim().isEmpty()) {
            return 0.0;
        }

        List<String> candidateTokens = tokenize(candidate);
        List<String> referenceTokens = tokenize(reference);

        if (candidateTokens.isEmpty() || referenceTokens.isEmpty()) {
            return 0.0;
        }

        // Calculate precision for 1-gram to 4-gram
        double[] precisions = new double[4];
        double[] weights = { 0.25, 0.25, 0.25, 0.25 };

        for (int n = 1; n <= 4; n++) {
            precisions[n - 1] = calculateNGramPrecision(candidateTokens, referenceTokens, n);
        }

        // Geometric mean of precisions
        double logSum = 0.0;
        for (int i = 0; i < 4; i++) {
            if (precisions[i] > 0) {
                logSum += weights[i] * Math.log(precisions[i]);
            } else {
                return 0.0; // If any precision is 0, BLEU is 0
            }
        }

        double geometricMean = Math.exp(logSum);

        // Brevity penalty
        double brevityPenalty = calculateBrevityPenalty(
                candidateTokens.size(),
                referenceTokens.size());

        return brevityPenalty * geometricMean;
    }

    private double calculateNGramPrecision(List<String> candidate,
            List<String> reference,
            int n) {
        Map<String, Integer> candidateNGrams = getNGrams(candidate, n);
        Map<String, Integer> referenceNGrams = getNGrams(reference, n);

        int matchCount = 0;
        int totalCount = 0;

        for (Map.Entry<String, Integer> entry : candidateNGrams.entrySet()) {
            String ngram = entry.getKey();
            int candidateCount = entry.getValue();
            int referenceCount = referenceNGrams.getOrDefault(ngram, 0);

            matchCount += Math.min(candidateCount, referenceCount);
            totalCount += candidateCount;
        }

        return totalCount > 0 ? (double) matchCount / totalCount : 0.0;
    }

    private Map<String, Integer> getNGrams(List<String> tokens, int n) {
        Map<String, Integer> ngrams = new HashMap<>();

        for (int i = 0; i <= tokens.size() - n; i++) {
            String ngram = String.join(" ", tokens.subList(i, i + n));
            ngrams.put(ngram, ngrams.getOrDefault(ngram, 0) + 1);
        }

        return ngrams;
    }

    private double calculateBrevityPenalty(int candidateLength, int referenceLength) {
        if (candidateLength > referenceLength) {
            return 1.0;
        }
        return Math.exp(1.0 - (double) referenceLength / candidateLength);
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
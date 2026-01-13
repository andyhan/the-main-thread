package com.example.domain;

public class EvaluationMetrics {
    public double contextRelevance;
    public double faithfulness;
    public double answerRelevance;
    public double bleuScore;
    public double semanticSimilarity;
    public double overallScore;

    public EvaluationMetrics() {
    }

    @Override
    public String toString() {
        return String.format(
                "Metrics[context=%.2f, faithfulness=%.2f, relevance=%.2f, bleu=%.2f, semantic=%.2f, overall=%.2f]",
                contextRelevance, faithfulness, answerRelevance, bleuScore, semanticSimilarity, overallScore);
    }
}
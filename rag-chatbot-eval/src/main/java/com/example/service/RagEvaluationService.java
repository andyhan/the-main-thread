package com.example.service;

import java.util.List;

import org.jboss.logging.Logger;

import com.example.domain.ChatRequest;
import com.example.domain.ChatResponse;
import com.example.domain.EvaluationMetrics;
import com.example.metrics.BleuCalculator;
import com.example.metrics.RagasEvaluator;
import com.example.metrics.SemanticSimilarityCalculator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RagEvaluationService {

    private static final Logger LOG = Logger.getLogger(RagEvaluationService.class);

    @Inject
    BankingChatbot chatbot;

    @Inject
    ContextExtractor contextExtractor;

    @Inject
    RagasEvaluator ragasEvaluator;

    @Inject
    BleuCalculator bleuCalculator;

    @Inject
    SemanticSimilarityCalculator semanticCalculator;

    /**
     * Process a chat request and evaluate the response
     */
    public ChatResponse processAndEvaluate(ChatRequest request) {
        LOG.infof("Processing question: %s", request.question);

        // Step 1: Extract relevant contexts
        List<String> contexts = contextExtractor.extractContexts(request.question, 3);
        LOG.infof("Retrieved %d context segments", contexts.size());

        // Step 2: Generate answer using RAG
        String answer = chatbot.chat(request.question);
        LOG.infof("Generated answer: %s", answer);

        // Step 3: Evaluate the response
        EvaluationMetrics metrics = evaluateResponse(
                request.question,
                answer,
                contexts,
                request.referenceAnswer);

        // Step 4: Build response
        ChatResponse response = new ChatResponse();
        response.question = request.question;
        response.answer = answer;
        response.retrievedContexts = contexts;
        response.metrics = metrics;

        LOG.infof("Evaluation complete: %s", metrics);

        return response;
    }

    /**
     * Evaluate a RAG response using multiple metrics
     */
    private EvaluationMetrics evaluateResponse(String question,
            String answer,
            List<String> contexts,
            String referenceAnswer) {
        EvaluationMetrics metrics = new EvaluationMetrics();

        // RAGAS Metrics (always computed)
        try {
            metrics.contextRelevance = ragasEvaluator.evaluateContextRelevance(question, contexts);
            metrics.faithfulness = ragasEvaluator.evaluateFaithfulness(answer, contexts);
            metrics.answerRelevance = ragasEvaluator.evaluateAnswerRelevance(question, answer);
        } catch (Exception e) {
            LOG.errorf("Error computing RAGAS metrics: %s", e.getMessage());
        }

        // Reference-based metrics (only if reference answer provided)
        if (referenceAnswer != null && !referenceAnswer.trim().isEmpty()) {
            try {
                metrics.bleuScore = bleuCalculator.calculateBleu(answer, referenceAnswer);
                metrics.semanticSimilarity = semanticCalculator.calculateSimilarity(answer, referenceAnswer);
            } catch (Exception e) {
                LOG.errorf("Error computing reference-based metrics: %s", e.getMessage());
            }
        }

        // Calculate overall score
        metrics.overallScore = calculateOverallScore(metrics);

        return metrics;
    }

    private double calculateOverallScore(EvaluationMetrics metrics) {
        double sum = metrics.contextRelevance + metrics.faithfulness + metrics.answerRelevance;
        int count = 3;

        if (metrics.bleuScore > 0) {
            sum += metrics.bleuScore;
            count++;
        }

        if (metrics.semanticSimilarity > 0) {
            sum += metrics.semanticSimilarity;
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }
}
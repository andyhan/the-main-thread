package com.example.metrics;

import java.util.List;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RagasEvaluator {

    @Inject
    ChatModel chatModel;

    /**
     * Context Relevance: How relevant are the retrieved documents to the question?
     */
    public double evaluateContextRelevance(String question, List<String> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return 0.0;
        }

        StringBuilder contextText = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            contextText.append("Context ").append(i + 1).append(": ")
                    .append(contexts.get(i)).append("\n\n");
        }

        String prompt = String.format("""
                Question: %s

                Retrieved Contexts:
                %s

                Task: Rate how relevant the provided contexts are to answering the question.
                Consider if the contexts contain information needed to answer the question.

                Respond with ONLY a number between 0.0 and 1.0, where:
                - 0.0 = completely irrelevant
                - 1.0 = perfectly relevant

                Score:""", question, contextText.toString());

        return parseScore(chatModel.chat(prompt));
    }

    /**
     * Faithfulness: Is the answer grounded in the provided context?
     */
    public double evaluateFaithfulness(String answer, List<String> contexts) {
        if (answer == null || answer.trim().isEmpty() || contexts == null || contexts.isEmpty()) {
            return 0.0;
        }

        StringBuilder contextText = new StringBuilder();
        for (String context : contexts) {
            contextText.append(context).append("\n\n");
        }

        String prompt = String.format("""
                Retrieved Context:
                %s

                Generated Answer: %s

                Task: Evaluate if the answer is faithful to the context.
                Check if all claims in the answer can be verified from the context.
                An answer is faithful if it doesn't add information not present in the context.

                Respond with ONLY a number between 0.0 and 1.0, where:
                - 0.0 = completely unfaithful (hallucinated content)
                - 1.0 = perfectly faithful (all claims supported by context)

                Score:""", contextText.toString(), answer);

        return parseScore(chatModel.chat(prompt));
    }

    /**
     * Answer Relevance: Does the answer actually address the question?
     */
    public double evaluateAnswerRelevance(String question, String answer) {
        if (question == null || answer == null ||
                question.trim().isEmpty() || answer.trim().isEmpty()) {
            return 0.0;
        }

        String prompt = String.format("""
                Question: %s

                Generated Answer: %s

                Task: Rate how well the answer addresses the question.
                Consider if the answer is on-topic and provides useful information.

                Respond with ONLY a number between 0.0 and 1.0, where:
                - 0.0 = completely irrelevant to the question
                - 1.0 = perfectly relevant and complete answer

                Score:""", question, answer);

        return parseScore(chatModel.chat(prompt));
    }

    private double parseScore(String response) {
        try {
            // Extract number from response
            String cleaned = response.trim()
                    .replaceAll("[^0-9.]", "")
                    .trim();

            if (cleaned.isEmpty()) {
                System.err.println("No numeric value found in response: " + response);
                return 0.5;
            }

            double score = Double.parseDouble(cleaned);
            return Math.max(0.0, Math.min(1.0, score)); // Clamp between 0 and 1
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse score from: " + response);
            return 0.5; // Default middle score if parsing fails
        }
    }
}
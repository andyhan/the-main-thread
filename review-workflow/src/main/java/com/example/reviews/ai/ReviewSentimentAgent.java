package com.example.reviews.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface ReviewSentimentAgent {

    @SystemMessage("""
            You classify product reviews for a customer support workflow.
            Return ONLY one label from this list:
            very-positive, positive, neutral, negative, very-negative
            """)
    String classify(@MemoryId String memoryId, @UserMessage String reviewText);
}
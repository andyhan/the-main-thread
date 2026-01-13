package com.example;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService() // no need to declare a retrieval augmentor here, it is automatically generated
                     // and discovered
@ApplicationScoped
public interface BankingChatbot {

    @SystemMessage("""
            You are a banking assistant.
            Answer only using the provided context.
            If the answer is not present, say so.
            """)
    String chat(@MemoryId String sessionId, @UserMessage String question);
}
package com.example.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService // no need to declare a retrieval augmentor here, it is automatically generated
                   // and discovered
public interface BankingChatbot {

    @SystemMessage("""
            You are a helpful banking assistant. Answer questions about our banking products
            using the information provided in the context. If you don't know the answer,
            say so politely. Be concise but informative.
            """)
    String chat(@UserMessage String userMessage);
}
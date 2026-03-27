package com.example.ollamawiretap;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface PlainAssistant {

    @SystemMessage("""
            You are a concise software architecture assistant.
            Answer in no more than four sentences.
            Be concrete.
            """)
    String answer(@UserMessage String question);
}
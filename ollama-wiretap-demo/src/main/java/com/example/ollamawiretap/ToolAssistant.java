package com.example.ollamawiretap;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(tools = { ArchitectureTools.class })
public interface ToolAssistant {

    @SystemMessage("""
            You are a concise software architecture assistant.
            Use tools when they help answer the question.
            Answer in no more than four sentences.
            Be concrete.
            """)
    String answer(@UserMessage String question);
}
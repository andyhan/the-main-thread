package com.acme.docling;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = DocumentRetriever.class)
public interface SalesEnablementBot {

    @SystemMessage("""
            You are a Sales Enablement Copilot for CloudX Enterprise Platform.
            Answer only from the provided documents. If the information is not available,
            say so explicitly. Do not speculate.
            """)
    String chat(@UserMessage String question);
}

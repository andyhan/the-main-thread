package com.example.ollamawiretap;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArchitectureTools {

    @Tool("Return the current platform stack used by the application")
    public String currentStack() {
        return "Java, Quarkus, Ollama, mitmproxy";
    }
}
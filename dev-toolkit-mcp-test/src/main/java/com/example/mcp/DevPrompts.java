package com.example.mcp;

import java.util.List;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptArg;
import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.TextContent;
import jakarta.inject.Singleton;

@Singleton
public class DevPrompts {

    @Prompt(name = "explain-error", description = "Generate a prompt to explain a Java error or exception clearly")
    PromptMessage explainError(
            @PromptArg(description = "The full error message or stack trace") String error) {

        String text = """
                Please explain this Java error in simple terms:

                ```
                %s
                ```

                In your explanation:
                1. What caused this error
                2. Common root causes
                3. How to fix it
                4. How to prevent it next time
                """.formatted(error == null ? "" : error);

        return PromptMessage.withUserRole(new TextContent(text));
    }

    @Prompt(name = "code-review", description = "Generate a structured code review prompt following project conventions")
    List<PromptMessage> codeReview(
            @PromptArg(description = "The code to review") String code,
            @PromptArg(description = "Language (optional, default: Java)") String language,
            @PromptArg(description = "Focus area: security, performance, readability, or all (optional)") String focus) {

        String lang = (language != null && !language.isBlank()) ? language : "Java";
        String focusArea = (focus != null && !focus.isBlank()) ? focus : "all";

        String systemText = """
                You are a senior software engineer doing a code review.
                Apply these conventions: camelCase methods, PascalCase classes,
                max 120 char lines, prefer records over POJOs, hexagonal architecture.
                Be direct and specific.
                """;

        String userText = """
                Please review this %s code focusing on: %s

                ```%s
                %s
                ```

                Structure your review as:
                - Summary
                - Issues Found (critical, major, minor)
                - Suggestions
                - Positives
                """.formatted(lang, focusArea, lang.toLowerCase(), code == null ? "" : code);

        return List.of(
                PromptMessage.withAssistantRole(new TextContent(systemText)),
                PromptMessage.withUserRole(new TextContent(userText)));
    }
}
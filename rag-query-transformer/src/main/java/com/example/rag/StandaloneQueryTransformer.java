package com.example.rag;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StandaloneQueryTransformer implements QueryTransformer {

    private static final Logger LOG = Logger.getLogger(StandaloneQueryTransformer.class);

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                     Rewrite the following question so it is fully self-contained.
                     Preserve the original intent.
                     Do not answer the question.
                     Do not add explanations.

                    Conversation:
                    {{chatMemory}}

                    User query: {{query}}

                     Rewritten question:
                     """);

    protected final PromptTemplate promptTemplate;
    protected final ChatModel chatModel;

    @Inject
    public StandaloneQueryTransformer(ChatModel chatModel) {
        this(chatModel, DEFAULT_PROMPT_TEMPLATE);
    }

    public StandaloneQueryTransformer(ChatModel chatModel, PromptTemplate promptTemplate) {
        this.chatModel = ensureNotNull(chatModel, "chatModel");
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    }

    @Override
    public Collection<Query> transform(Query query) {
        String originalQuery = query.text();
        LOG.infof("Transforming query (before): %s", originalQuery);

        long startTime = System.currentTimeMillis();

        List<ChatMessage> chatMemory = query.metadata() != null ? query.metadata().chatMemory() : null;
        if (chatMemory == null || chatMemory.isEmpty()) {
            // no need to compress if there are no previous messages
            long duration = System.currentTimeMillis() - startTime;
            LOG.infof("Query unchanged (no chat memory), took %d ms", duration);
            return singletonList(query);
        }

        Prompt prompt = createPrompt(query, format(chatMemory));
        String compressedQueryText = chatModel.chat(prompt.text());
        Query compressedQuery = query.metadata() == null
                ? Query.from(compressedQueryText)
                : Query.from(compressedQueryText, query.metadata());

        long duration = System.currentTimeMillis() - startTime;
        LOG.infof("Transforming query (after): %s", compressedQueryText);
        LOG.infof("Query transformation took %d ms", duration);

        return singletonList(compressedQuery);
    }

    protected String format(List<ChatMessage> chatMemory) {
        return chatMemory.stream()
                .map(this::format)
                .filter(Objects::nonNull)
                .collect(joining("\n"));
    }

    protected String format(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return "User: " + userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            if (aiMessage.hasToolExecutionRequests()) {
                return null;
            }
            return "AI: " + aiMessage.text();
        } else {
            return null;
        }
    }

    protected Prompt createPrompt(Query query, String chatMemory) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query.text());
        variables.put("chatMemory", chatMemory);
        return promptTemplate.apply(variables);
    }

}
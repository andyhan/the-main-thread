package com.ibm.ai;

import java.util.function.Supplier;

import com.ibm.retrieval.DocumentRetriever;

import dev.langchain4j.rag.RetrievalAugmentor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Wires the custom {@link RetrievalAugmentor} into the Quarkus LangChain4j AI service.
 */
@ApplicationScoped
public class DocumentRetrieverAugmentorSupplier implements Supplier<RetrievalAugmentor> {

    private final DocumentRetriever documentRetriever;

    @Inject
    public DocumentRetrieverAugmentorSupplier(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
    }

    @Override
    public RetrievalAugmentor get() {
        return documentRetriever;
    }
}

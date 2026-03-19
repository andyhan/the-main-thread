package com.acme.docling;

import java.util.function.Supplier;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DocumentRetriever implements RetrievalAugmentor, Supplier<RetrievalAugmentor> {
    private final RetrievalAugmentor augmentor;

    DocumentRetriever(EmbeddingStore<TextSegment> store, EmbeddingModel model) {
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(5)
                .build();
        augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();
    }

    @Override
    public RetrievalAugmentor get() {
        return this;
    }

    @Override
    public AugmentationResult augment(AugmentationRequest request) {
        return augmentor.augment(request);
    }
}

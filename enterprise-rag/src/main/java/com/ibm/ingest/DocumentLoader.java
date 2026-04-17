package com.ibm.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Loads documents from {@code documents/}, converts them with Docling (async
 * task API), splits, and
 * stores embeddings. Runs in the background after startup;
 * {@link IndexingState} and readiness reflect
 * completion.
 */
@ApplicationScoped
public class DocumentLoader {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("txt", "pdf", "pptx", "ppt", "doc", "docx",
            "xlsx", "xls", "csv", "json", "xml", "html");

    @Inject
    EmbeddingStore<TextSegment> store;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    DoclingConverter doclingConverter;

    @Inject
    IndexingState indexingState;

    public void startAsyncIngestion() {
        indexingState.setIndexReady(false);
        Log.info("Starting document loading (background)...");

        listEligiblePathsUni()
                .chain(paths -> {
                    if (paths.isEmpty()) {
                        Log.warn("No documents to process. Skipping embedding generation.");
                        return Uni.createFrom().voidItem();
                    }
                    return Multi.createFrom().iterable(paths)
                            .onItem().transformToUniAndConcatenate(path -> doclingConverter.convertToMarkdownUni(path)
                                    .map(markdown -> toDocument(path, markdown)))
                            .collect().asList()
                            .chain(this::embedAllDocuments);
                })
                .subscribe().with(
                        ignored -> finishIngestionSuccess(),
                        this::finishIngestionFailure);
    }

    private void finishIngestionSuccess() {
        indexingState.setIndexReady(true);
        Log.info("Document ingestion pipeline finished; readiness is UP.");
    }

    private void finishIngestionFailure(Throwable failure) {
        Log.error("Document ingestion pipeline failed; readiness set UP so the app is not stuck DOWN.", failure);
        indexingState.setIndexReady(true);
    }

    private Uni<List<Path>> listEligiblePathsUni() {
        return Uni.createFrom().item(() -> {
            Path documentsPath = Path.of("src/main/resources/documents");
            List<Path> paths = new ArrayList<>();
            if (!Files.isDirectory(documentsPath)) {
                Log.warnf("Documents directory not found or not a directory: %s", documentsPath);
                return paths;
            }
            int skippedCount = 0;
            try (var stream = Files.list(documentsPath)) {
                for (Path filePath : stream.filter(Files::isRegularFile).toList()) {
                    String fileName = filePath.getFileName().toString();
                    String extension = fileExtension(fileName);
                    if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
                        skippedCount++;
                        Log.debugf("Skipping file '%s' - extension '%s' is not in allowed list",
                                fileName, extension.isEmpty() ? "(no extension)" : extension);
                        continue;
                    }
                    paths.add(filePath);
                }
            } catch (IOException e) {
                Log.errorf(e, "Failed to list documents in %s", documentsPath);
            }
            Log.infof("Found %d file(s) to process (%d skipped by extension).", paths.size(), skippedCount);
            return paths;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private static String fileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    private static Document toDocument(Path filePath, String markdown) {
        String fileName = filePath.getFileName().toString();
        String extension = fileExtension(fileName);
        Map<String, String> meta = new HashMap<>();
        meta.put("file", fileName);
        meta.put("format", extension);
        return Document.document(markdown, new Metadata(meta));
    }

    private Uni<Void> embedAllDocuments(List<Document> docs) {
        if (docs.isEmpty()) {
            Log.warn("No documents were successfully converted. Skipping embedding generation.");
            return Uni.createFrom().voidItem();
        }

        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(200, 20);
        List<TextSegment> segments = splitter.splitAll(docs);

        if (segments.isEmpty()) {
            Log.warn("No text segments generated from documents. Skipping embedding storage.");
            return Uni.createFrom().voidItem();
        }

        Log.infof("Generating embeddings for %d text segments...", segments.size());

        return Uni.createFrom().item(() -> {
            embedSegmentsBlocking(segments);
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).replaceWithVoid();
    }

    private void embedSegmentsBlocking(List<TextSegment> segments) {
        int embeddedCount = 0;
        int errorCount = 0;
        try {
            if (!segments.isEmpty()) {
                TextSegment testSegment = segments.get(0);
                var testEmbedding = embeddingModel.embed(testSegment).content();
                store.add(testEmbedding, testSegment);
                Log.infof("Store test successful. Proceeding with bulk embedding...");
                embeddedCount = 1;
            }
        } catch (jakarta.enterprise.inject.CreationException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException
                    && cause.getMessage() != null
                    && cause.getMessage().contains("indexListSize")
                    && cause.getMessage().contains("zero")) {
                Log.errorf("PgVector dimension configuration error detected during store initialization.");
                Log.errorf("The dimension property 'quarkus.langchain4j.pgvector.dimension' is being read as 0.");
                throw new RuntimeException(
                        "PgVector store initialization failed. Check application.properties and database configuration.",
                        e);
            }
            throw e;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("indexListSize") && e.getMessage().contains("zero")) {
                Log.errorf("PgVector dimension configuration error. The dimension is being read as 0.");
                throw new RuntimeException(
                        "PgVector dimension misconfiguration. Dimension must be > 0. Check application.properties.", e);
            }
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Failed to test embedding store. This might indicate a configuration issue.");
            throw new RuntimeException(
                    "Embedding store test failed. Please check your database and pgvector configuration.", e);
        }

        int startIndex = embeddedCount > 0 ? 1 : 0;
        for (int i = startIndex; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            try {
                var embedding = embeddingModel.embed(segment).content();
                store.add(embedding, segment);
                embeddedCount++;
                if (embeddedCount % 10 == 0) {
                    Log.infof("Progress: embedded %d/%d segments", embeddedCount, segments.size());
                }
            } catch (Exception e) {
                errorCount++;
                Log.errorf(e, "Failed to embed and store segment: %s",
                        segment.text().substring(0, Math.min(50, segment.text().length())));
            }
        }

        Log.infof("Successfully embedded and stored %d out of %d segments (errors: %d)", embeddedCount,
                segments.size(), errorCount);
    }
}

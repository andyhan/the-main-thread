package com.acme.docling.ingest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DocumentLoader {

    private static final Executor VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    EmbeddingStore<TextSegment> store;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    DoclingConverter converter;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "txt", "pdf", "pptx", "ppt", "doc", "docx",
            "xlsx", "xls", "csv", "json", "xml", "html");

    /** Batch size for Ollama embedding calls (fewer round-trips than one-by-one). */
    private static final int EMBED_BATCH_SIZE = 32;

    /** Plain-text / Markdown sources on the classpath (works in dev and in packaged runners). */
    private static final List<String> CLASSPATH_TEXT_DOCS = List.of(
            "documents/cloudx-enterprise.md");

    /**
     * Quarkus only allows {@code @RunOnVirtualThread} on framework entrypoints (REST, etc.), not on
     * {@code @Observes} methods. We offload onto {@link #VIRTUAL_THREAD_EXECUTOR} so classpath I/O and
     * pipeline wiring do not run on the Vert.x event loop. Docling completes via {@link CompletionStage}
     * without {@code join()}; embedding runs on the same executor in {@code thenAcceptAsync}.
     */
    void onStart(@Observes StartupEvent event) {
        VIRTUAL_THREAD_EXECUTOR.execute(() -> {
            try {
                ingestDocumentsAsync().whenComplete((ok, err) -> {
                    if (err != null) {
                        Log.errorf(unwrap(err), "Document ingestion failed");
                    }
                });
            } catch (Exception e) {
                Log.errorf(e, "Document ingestion failed");
            }
        });
    }

    private CompletionStage<Void> ingestDocumentsAsync() {
        Log.info("Starting document ingestion...");
        List<Document> docs = new ArrayList<>();

        ingestClasspathTextDocs(docs);

        CompletionStage<List<Document>> withLocal;
        Path localDocs = Path.of("src/main/resources/documents");
        if (Files.exists(localDocs) && Files.isDirectory(localDocs)) {
            withLocal = ingestLocalFilesAsync(localDocs, docs);
        } else {
            withLocal = CompletableFuture.completedFuture(docs);
        }

        return withLocal
                .thenCompose(this::ingestRemoteUrlsAsync)
                .thenAcceptAsync(finalDocs -> {
                    if (finalDocs.isEmpty()) {
                        Log.warn("No documents ingested.");
                        return;
                    }
                    embedAndStore(finalDocs);
                    Log.info("Startup document ingestion complete. Quarkus may already accept HTTP traffic; until this line appears, /bot can see an empty or partial index.");
                }, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Convert a remote URL via Docling Serve and embed the resulting Markdown (async end-to-end).
     */
    public CompletionStage<Void> ingestRemoteDocumentAsync(URI uri) {
        return converter.toMarkdownFromUrlAsync(uri)
                .thenAcceptAsync(markdown -> {
                    Map<String, String> meta = new HashMap<>();
                    meta.put("source", uri.toString());
                    meta.put("format", "remote");
                    ingestMarkdownDocument(markdown, new Metadata(meta));
                }, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Starts {@link #ingestRemoteDocumentAsync(URI)} and returns immediately. Completion or failure is
     * logged only—use for HTTP handlers that should answer before ingestion finishes.
     */
    public void ingestRemoteDocumentInBackground(URI uri) {
        ingestRemoteDocumentAsync(uri).whenComplete((ok, err) -> {
            if (err != null) {
                Log.errorf(unwrap(err), "Background URL ingest failed: %s", uri);
            } else {
                Log.infof("Background URL ingest finished: %s", uri);
            }
        });
    }

    /**
     * Blocking convenience for callers that need to wait for completion (uses {@code join()}).
     */
    public void ingestRemoteDocument(URI uri) throws IOException {
        try {
            ingestRemoteDocumentAsync(uri).toCompletableFuture().join();
        } catch (CompletionException e) {
            Throwable c = unwrap(e);
            if (c instanceof IOException ioe) {
                throw ioe;
            }
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new IOException(c);
        }
    }

    /**
     * Chunk and embed a single logical document (used for runtime ingestion).
     */
    public void ingestMarkdownDocument(String markdown, Metadata metadata) {
        embedAndStore(List.of(Document.document(markdown, metadata)));
    }

    private void ingestClasspathTextDocs(List<Document> docs) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String resource : CLASSPATH_TEXT_DOCS) {
            try (InputStream in = cl.getResourceAsStream(resource)) {
                if (in == null) {
                    Log.warnf("Classpath document not found: %s", resource);
                    continue;
                }
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (text.isBlank()) {
                    Log.warnf("Classpath document is empty: %s", resource);
                    continue;
                }
                Map<String, String> meta = new HashMap<>();
                meta.put("source", resource);
                meta.put("format", "classpath-md");
                docs.add(Document.document(text, new Metadata(meta)));
                Log.infof("Loaded classpath document: %s (%d chars)", resource, text.length());
            } catch (Exception e) {
                Log.errorf(e, "Failed to load classpath document: %s", resource);
            }
        }
    }

    private CompletionStage<List<Document>> ingestLocalFilesAsync(Path directory, List<Document> seedDocs) {
        final List<Path> paths;
        try (var stream = Files.list(directory)) {
            paths = stream.filter(Files::isRegularFile).toList();
        } catch (Exception e) {
            Log.errorf(e, "Failed to read documents directory");
            return CompletableFuture.completedFuture(seedDocs);
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        CompletionStage<List<Document>> chain = CompletableFuture.completedFuture(new ArrayList<>(seedDocs));
        for (Path filePath : paths) {
            chain = chain.thenCompose(docs -> ingestOneLocalFile(filePath, docs, success, failure, skipped));
        }
        return chain.thenApply(docs -> {
            Log.infof("Local ingestion: success=%d, failures=%d, skipped=%d",
                    success.get(), failure.get(), skipped.get());
            return docs;
        });
    }

    private CompletionStage<List<Document>> ingestOneLocalFile(
            Path filePath,
            List<Document> docs,
            AtomicInteger success,
            AtomicInteger failure,
            AtomicInteger skipped) {
        File file = filePath.toFile();
        String name = file.getName();
        String ext = extension(name);

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            skipped.incrementAndGet();
            return CompletableFuture.completedFuture(docs);
        }

        return converter.toMarkdownAsync(file)
                .handle((markdown, err) -> {
                    if (err != null) {
                        failure.incrementAndGet();
                        Log.errorf(unwrap(err), "Failed to process: %s", name);
                        return docs;
                    }
                    Map<String, String> meta = new HashMap<>();
                    meta.put("file", name);
                    meta.put("format", ext);
                    docs.add(Document.document(markdown, new Metadata(meta)));
                    success.incrementAndGet();
                    return docs;
                });
    }

    private CompletionStage<List<Document>> ingestRemoteUrlsAsync(List<Document> docs) {
        List<URI> remoteDocuments = loadRemoteDocumentUris();
        CompletionStage<List<Document>> chain = CompletableFuture.completedFuture(docs);
        for (URI uri : remoteDocuments) {
            chain = chain.thenCompose(d -> ingestOneRemote(uri, d));
        }
        return chain;
    }

    private CompletionStage<List<Document>> ingestOneRemote(URI uri, List<Document> docs) {
        Log.infof("Ingesting remote document: %s", uri);
        return converter.toMarkdownFromUrlAsync(uri)
                .handle((markdown, err) -> {
                    if (err != null) {
                        Log.errorf(unwrap(err), "Failed to ingest remote document: %s", uri);
                        return docs;
                    }
                    Map<String, String> meta = new HashMap<>();
                    meta.put("source", uri.toString());
                    meta.put("format", "remote");
                    docs.add(Document.document(markdown, new Metadata(meta)));
                    return docs;
                });
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }

    private void embedAndStore(List<Document> docs) {
        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(200, 20);
        List<TextSegment> segments = splitter.splitAll(docs);

        if (segments.isEmpty()) {
            Log.warn("No segments generated from documents.");
            return;
        }

        Log.infof("Embedding %d segments in batches of %d...", segments.size(), EMBED_BATCH_SIZE);
        int stored = 0;
        int errors = 0;

        for (int start = 0; start < segments.size(); start += EMBED_BATCH_SIZE) {
            int end = Math.min(start + EMBED_BATCH_SIZE, segments.size());
            List<TextSegment> batch = new ArrayList<>(segments.subList(start, end));
            try {
                List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                if (embeddings == null || embeddings.size() != batch.size()) {
                    throw new IllegalStateException("embedAll returned "
                            + (embeddings == null ? 0 : embeddings.size())
                            + " embeddings for batch size "
                            + batch.size());
                }
                store.addAll(embeddings, batch);
                stored += batch.size();
                if (stored % 100 == 0 || stored == segments.size()) {
                    Log.infof("Embedded %d/%d", stored, segments.size());
                }
            } catch (Exception e) {
                Log.errorf(e, "Batch embed/store failed for segments %d-%d; retrying one segment at a time",
                        start, end - 1);
                for (TextSegment segment : batch) {
                    try {
                        store.add(embeddingModel.embed(segment).content(), segment);
                        stored++;
                    } catch (Exception e2) {
                        errors++;
                        Log.errorf(e2, "Embedding failed for segment");
                    }
                }
            }
        }
        Log.infof("Embedding complete: %d stored, %d errors", stored, errors);
        if (errors > 0) {
            Log.warn("Some segments failed to embed; the index is partial. Fix upstream content or re-run ingestion before relying on retrieval.");
        }
    }

    private List<URI> loadRemoteDocumentUris() {
        // In a real application, pull this from configuration or a database.
        // For this tutorial, return an empty list unless you have remote docs to test.
        return List.of();
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1)
                ? filename.substring(dot + 1).toLowerCase()
                : "";
    }
}

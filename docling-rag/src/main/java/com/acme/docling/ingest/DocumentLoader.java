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

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Startup
public class DocumentLoader {

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
     * Quarkus only allows {@code @RunOnVirtualThread} on framework entrypoints
     * (REST,
     * schedulers, etc.), not on {@code @PostConstruct}. We start a platform virtual
     * thread here instead so blocking DoclingServeApi (async submit-and-poll) work does not run on the
     * Vert.x
     * event loop.
     */
    @PostConstruct
    void startIngestionOnVirtualThread() {
        Thread.ofVirtual()
                .name("document-ingestion")
                .start(() -> {
                    try {
                        ingestDocuments();
                    } catch (Exception e) {
                        Log.errorf(e, "Document ingestion failed");
                    }
                });
    }

    private void ingestDocuments() throws Exception {
        Log.info("Starting document ingestion...");
        List<Document> docs = new ArrayList<>();

        ingestClasspathTextDocs(docs);

        // Optional: project-relative path when running `mvn quarkus:dev` from the repo root
        Path localDocs = Path.of("src/main/resources/documents");
        if (Files.exists(localDocs) && Files.isDirectory(localDocs)) {
            ingestLocalFiles(localDocs, docs);
        }

        // Path 2: remote URLs from a config file or hardcoded list
        // In production, this list would come from a database or config source
        List<URI> remoteDocuments = loadRemoteDocumentUris();
        for (URI uri : remoteDocuments) {
            try {
                Log.infof("Ingesting remote document: %s", uri);
                String markdown = converter.toMarkdownFromUrl(uri);
                Map<String, String> meta = new HashMap<>();
                meta.put("source", uri.toString());
                meta.put("format", "remote");
                docs.add(Document.document(markdown, new Metadata(meta)));
            } catch (Exception e) {
                Log.errorf(e, "Failed to ingest remote document: %s", uri);
            }
        }

        if (docs.isEmpty()) {
            Log.warn("No documents ingested.");
            return;
        }

        embedAndStore(docs);
        Log.info("Startup document ingestion complete. Quarkus may already accept HTTP traffic; until this line appears, /bot can see an empty or partial index.");
    }

    /**
     * Convert a remote URL via Docling Serve and embed the resulting Markdown (same pipeline as startup).
     */
    public void ingestRemoteDocument(URI uri) throws IOException {
        String markdown = converter.toMarkdownFromUrl(uri);
        Map<String, String> meta = new HashMap<>();
        meta.put("source", uri.toString());
        meta.put("format", "remote");
        ingestMarkdownDocument(markdown, new Metadata(meta));
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

    private void ingestLocalFiles(Path directory, List<Document> docs) {
        int success = 0, failure = 0, skipped = 0;
        try (var stream = Files.list(directory)) {
            for (Path filePath : stream.filter(Files::isRegularFile).toList()) {
                File file = filePath.toFile();
                String name = file.getName();
                String ext = extension(name);

                if (!ALLOWED_EXTENSIONS.contains(ext)) {
                    skipped++;
                    continue;
                }

                try {
                    String markdown = converter.toMarkdown(file);
                    Map<String, String> meta = new HashMap<>();
                    meta.put("file", name);
                    meta.put("format", ext);
                    docs.add(Document.document(markdown, new Metadata(meta)));
                    success++;
                } catch (Exception e) {
                    failure++;
                    Log.errorf(e, "Failed to process: %s", name);
                }
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to read documents directory");
        }
        Log.infof("Local ingestion: success=%d, failures=%d, skipped=%d", success, failure, skipped);
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

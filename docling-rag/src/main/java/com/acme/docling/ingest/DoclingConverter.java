package com.acme.docling.ingest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.CompletionException;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Converts documents to Markdown using the DoclingServeApi.
 *
 * <p>Uses {@link DoclingServeApi#convertSourceAsync} so the Quarkus implementation
 * ({@code QuarkusDoclingServeApi}) runs Docling Serve’s submit-and-poll flow:
 * {@code POST /v1/convert/source/async}, then periodic task status checks until the
 * result is ready (see {@code quarkus.docling.async-poll-interval} and
 * {@code quarkus.docling.async-timeout}). We block on
 * {@code convertSourceAsync(...).toCompletableFuture().join()} here on purpose; callers run on virtual threads.</p>
 *
 * <p>Supports two input types:
 * <ul>
 * <li>Local files as Base64 {@code FileSource}</li>
 * <li>Remote URLs as {@code HttpSource} (Docling Serve fetches the bytes)</li>
 * </ul>
 */
@ApplicationScoped
public class DoclingConverter {

    @Inject
    DoclingServeApi doclingServeApi;

    /**
     * Convert a local file to Markdown.
     * The file is Base64-encoded and sent to Docling Serve as a FileSource.
     */
    public String toMarkdown(File sourceFile) throws IOException {
        String filename = sourceFile.getName();
        Log.infof("Converting local file: %s", filename);

        try {
            byte[] bytes = Files.readAllBytes(sourceFile.toPath());
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            var request = ConvertDocumentRequest.builder()
                    .source(FileSource.builder()
                            .filename(filename)
                            .base64String(base64)
                            .build())
                    .build();

            var response = convertWithAsyncPoll(request, sourceFile.getAbsolutePath());
            return extractMarkdown(response, filename);

        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to convert local file: " + sourceFile, e);
        }
    }

    /**
     * Convert a remote document to Markdown.
     * Docling Serve fetches the document from the URL itself — nothing is
     * downloaded
     * locally. This is the preferred path for documents already stored in a remote
     * location (S3, SharePoint, internal document server, public URLs).
     */
    public String toMarkdownFromUrl(URI documentUri) throws IOException {
        Log.infof("Converting remote document: %s", documentUri);

        try {
            var request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder()
                            .url(documentUri)
                            .build())
                    .build();

            var response = convertWithAsyncPoll(request, documentUri.toString());
            return extractMarkdown(response, documentUri.toString());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to convert remote document: " + documentUri, e);
        }
    }

    /**
     * Submits async conversion and blocks until polling completes (Quarkus worker pool + configured poll interval).
     */
    private ConvertDocumentResponse convertWithAsyncPoll(ConvertDocumentRequest request, String sourceLabel)
            throws IOException {
        try {
            return doclingServeApi.convertSourceAsync(request).toCompletableFuture().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IOException ioe) {
                throw ioe;
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IOException("Docling async conversion failed for: " + sourceLabel, cause);
        }
    }

    private String extractMarkdown(ConvertDocumentResponse response, String source) {
        String markdown = response.getDocument().getMarkdownContent();
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalStateException("Conversion returned no Markdown for: " + source);
        }
        Log.infof("Conversion complete: %s (%d chars of Markdown)", source, markdown.length());
        return markdown;
    }
}

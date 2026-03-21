package com.acme.docling.ingest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import ai.docling.serve.api.convert.response.ResponseType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Converts documents to Markdown using the DoclingServeApi.
 *
 * <p>Uses {@link DoclingServeApi#convertSourceAsync} so Docling Serve’s submit-and-poll flow runs
 * without {@code CompletableFuture.join()} in this class: conversion is chained as a
 * {@link CompletionStage}. Local file reads are offloaded with {@link CompletableFuture#supplyAsync}
 * so blocking I/O does not run on the Vert.x event loop.</p>
 */
@ApplicationScoped
public class DoclingConverter {

    private static final Executor VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    DoclingServeApi doclingServeApi;

    /**
     * Convert a local file to Markdown (async Docling call; file read on a virtual thread).
     */
    public CompletionStage<String> toMarkdownAsync(File sourceFile) {
        String filename = sourceFile.getName();
        Log.infof("Converting local file: %s", filename);
        String sourceLabel = sourceFile.getAbsolutePath();

        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = Files.readAllBytes(sourceFile.toPath());
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                return ConvertDocumentRequest.builder()
                        .source(FileSource.builder()
                                .filename(filename)
                                .base64String(base64)
                                .build())
                        .build();
            } catch (IOException e) {
                throw new CompletionException(new IOException("Failed to read local file: " + sourceFile, e));
            }
        }, VIRTUAL_THREAD_EXECUTOR).thenCompose(request -> doclingServeApi.convertSourceAsync(request))
                .thenApply(response -> extractMarkdown(response, sourceLabel));
    }

    /**
     * Convert a remote document to Markdown. Docling Serve fetches the URL; no local binary download.
     */
    public CompletionStage<String> toMarkdownFromUrlAsync(URI documentUri) {
        Log.infof("Converting remote document: %s", documentUri);
        var request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(documentUri)
                        .build())
                .build();
        return doclingServeApi.convertSourceAsync(request)
                .thenApply(response -> extractMarkdown(response, documentUri.toString()));
    }

    /**
     * Blocking convenience for callers that need a plain {@link String} (uses {@code join()}).
     */
    public String toMarkdown(File sourceFile) throws IOException {
        try {
            return toMarkdownAsync(sourceFile).toCompletableFuture().join();
        } catch (CompletionException e) {
            throw unwrapIoException(e);
        }
    }

    /**
     * Blocking convenience for callers that need a plain {@link String} (uses {@code join()}).
     */
    public String toMarkdownFromUrl(URI documentUri) throws IOException {
        try {
            return toMarkdownFromUrlAsync(documentUri).toCompletableFuture().join();
        } catch (CompletionException e) {
            throw unwrapIoException(e);
        }
    }

    private static IOException unwrapIoException(CompletionException e) {
        Throwable c = e.getCause() != null ? e.getCause() : e;
        if (c instanceof IOException ioe) {
            return ioe;
        }
        if (c instanceof RuntimeException re) {
            throw re;
        }
        return new IOException(c);
    }

    private String extractMarkdown(ConvertDocumentResponse response, String source) {
        if (response.getResponseType() != ResponseType.IN_BODY || !(response instanceof InBodyConvertDocumentResponse inBody)) {
            throw new IllegalStateException("Expected in-body conversion for: " + source + ", got " + response.getResponseType());
        }
        String markdown = inBody.getDocument().getMarkdownContent();
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalStateException("Conversion returned no Markdown for: " + source);
        }
        Log.infof("Conversion complete: %s (%d chars of Markdown)", source, markdown.length());
        return markdown;
    }
}

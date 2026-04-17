package com.ibm.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import io.quarkiverse.docling.runtime.client.ApiMetadata;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeClient;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

/**
 * Converts files to Markdown via Docling Serve using the Quarkus client's async
 * task API
 * ({@link QuarkusDoclingServeClient#submitConvertSourceAsync}) and polling
 * until completion.
 */
@ApplicationScoped
public class DoclingConverter {

    private final QuarkusDoclingServeClient doclingClient;
    private final ApiMetadata apiMetadata;

    @Inject
    public DoclingConverter(QuarkusDoclingServeClient doclingClient, DoclingRuntimeConfig doclingConfig) {
        this.doclingClient = doclingClient;
        ApiMetadata.Builder metadata = ApiMetadata.builder();
        doclingConfig.apiKey().ifPresent(metadata::apiKey);
        this.apiMetadata = metadata.build();
    }

    /**
     * Converts a file to Markdown asynchronously (Mutiny). Subscription runs on the
     * default worker pool
     * so polling and JAX-RS client calls do not block the event loop. Read errors
     * become a failed {@link Uni}
     * so callers can use this from lambdas without handling checked exceptions.
     */
    public Uni<String> convertToMarkdownUni(Path filePath) {
        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            return Uni.createFrom().failure(e);
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String filename = filePath.getFileName().toString();

        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .source(FileSource.builder()
                        .base64String(base64)
                        .filename(filename)
                        .build())
                .options(ConvertDocumentOptions.builder()
                        .toFormat(OutputFormat.MARKDOWN)
                        .build())
                .target(InBodyTarget.builder().build())
                .build();

        return doclingClient.submitConvertSourceAsync(request, apiMetadata)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .chain(this::pollUntilSuccess)
                .chain(this::fetchMarkdownFromTask);
    }

    private Uni<TaskStatusPollResponse> pollUntilSuccess(TaskStatusPollResponse status) {
        TaskStatus t = status.getTaskStatus();
        if (t == TaskStatus.SUCCESS) {
            return Uni.createFrom().item(status);
        }
        if (t == TaskStatus.FAILURE) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "Docling conversion task failed for taskId=" + status.getTaskId()));
        }
        String taskId = status.getTaskId();
        return Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofMillis(200))
                .chain(ignored -> Uni.createFrom().item(() -> doclingClient.pollTaskStatus(taskId, 500L, apiMetadata))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .chain(this::pollUntilSuccess));
    }

    private Uni<String> fetchMarkdownFromTask(TaskStatusPollResponse completed) {
        String taskId = completed.getTaskId();
        return Uni.createFrom().item(() -> {
            Response response = doclingClient.convertTaskResult(taskId, apiMetadata);
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new ProcessingException(
                        "convertTaskResult failed: HTTP " + response.getStatus() + " for taskId=" + taskId);
            }
            InBodyConvertDocumentResponse inBody = response.readEntity(InBodyConvertDocumentResponse.class);
            var document = Objects.requireNonNull(inBody.getDocument(),
                    "Document conversion returned null document for taskId=" + taskId);
            return document.getMarkdownContent();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}

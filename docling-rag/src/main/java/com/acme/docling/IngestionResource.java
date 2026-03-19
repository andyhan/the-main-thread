package com.acme.docling;

import java.net.URI;

import com.acme.docling.ingest.DocumentLoader;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/ingest")
public class IngestionResource {

    @Inject
    DocumentLoader documentLoader;

    @POST
    @Path("/url")
    @Consumes(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response ingestUrl(RemoteIngestRequest request) throws Exception {
        if (request == null || request.url() == null || request.url().isBlank()) {
            throw new BadRequestException("url must be provided");
        }

        URI uri;
        try {
            uri = URI.create(request.url().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid url: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || uri.getHost() == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new BadRequestException("url must be an absolute http or https URL");
        }

        documentLoader.ingestRemoteDocument(uri);

        return Response.accepted().build();
    }
}

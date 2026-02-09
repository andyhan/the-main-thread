package com.example.resource;

import com.example.service.CompressionService;
import com.example.service.LogGeneratorService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {

    @Inject
    CompressionService compression;

    @Inject
    LogGeneratorService generator;

    @GET
    public Snapshot snapshot() {
        return new Snapshot(
                generator.totalLogs(),
                compression.operations(),
                compression.rawBytes(),
                compression.compressedBytes(),
                compression.ratio());
    }

    @POST
    @Path("/reset")
    public void reset() {
        compression.reset();
        generator.reset();
    }

    public record Snapshot(
            long logs,
            long operations,
            long rawBytes,
            long compressedBytes,
            double ratio) {
    }
}
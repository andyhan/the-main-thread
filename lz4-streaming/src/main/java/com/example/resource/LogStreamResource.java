package com.example.resource;

import java.time.Duration;
import java.util.Base64;

import org.jboss.resteasy.reactive.RestStreamElementType;

import com.example.service.CompressionService;
import com.example.service.LogGeneratorService;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)
public class LogStreamResource {

    @Inject
    LogGeneratorService generator;

    @Inject
    CompressionService compression;

    @GET
    @Path("/raw")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> raw(@QueryParam("rate") @DefaultValue("10") int rate) {
        return generator.stream(rate);
    }

    @GET
    @Path("/compressed")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> compressed(
            @QueryParam("rate") @DefaultValue("10") int rate,
            @QueryParam("batch") @DefaultValue("50") int batch) {
        long interval = (batch * 1000L) / rate;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(interval))
                .map(tick -> {
                    String logs = generator.batch(batch);
                    byte[] data = compression.compressWithHeader(logs);
                    return Base64.getEncoder().encodeToString(data);
                });
    }
}
package com.example;

import java.nio.file.Files;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/generate")
public class ImageGeneratorResource {

    @Inject
    Flux2Service service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(GenerateRequest request) {
        var result = service.generate(
                request.prompt(),
                request.width(),
                request.height(),
                request.steps());

        return Response.ok(
                new GenerateResponse(
                        result.filename(),
                        "/api/generate/image/" + result.filename(),
                        "success"))
                .build();
    }

    @GET
    @Path("/image/{filename}")
    @Produces("image/png")
    public Response image(@PathParam("filename") String filename) {
        java.nio.file.Path path = java.nio.file.Path.of(service.getOutputDir(), filename);

        if (!Files.exists(path)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(path.toFile()).build();
    }

    record GenerateResponse(String filename, String url, String status) {
    }
}
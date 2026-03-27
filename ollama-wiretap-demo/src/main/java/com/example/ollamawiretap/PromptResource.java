package com.example.ollamawiretap;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/inspect")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PromptResource {

    @Inject
    PlainAssistant plainAssistant;

    @Inject
    ToolAssistant toolAssistant;

    @POST
    @Path("/{mode}")
    public PromptResponse inspect(@PathParam("mode") String mode, PromptRequest request) {
        long start = System.currentTimeMillis();

        String answer = switch (mode) {
            case "plain" -> plainAssistant.answer(request.question());
            case "tool" -> toolAssistant.answer(request.question());
            default -> throw new BadRequestException("Mode must be 'plain' or 'tool'");
        };

        long duration = System.currentTimeMillis() - start;
        return new PromptResponse(mode, answer, duration);
    }
}
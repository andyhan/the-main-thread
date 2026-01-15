package com.secureai.resource;

import com.secureai.service.DefenseService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/secure-chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DefenseResource {

    @Inject
    DefenseService defenseService;

    @POST
    public Response chat(ChatRequest request) {
        try {
            // Pass raw input into our defense pipeline
            String response = defenseService.processSecurely(request.message());
            return Response.ok(new ChatResponse(response)).build();
        } catch (SecurityException e) {
            // If StruQ detects a spoofing attempt (very rare)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ChatResponse("Security Alert: " + e.getMessage()))
                    .build();
        }
    }

    // Simple DTOs
    public record ChatRequest(String message) {
    }

    public record ChatResponse(String reply) {
    }
}
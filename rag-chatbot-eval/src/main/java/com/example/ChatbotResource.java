package com.example;

import org.jboss.logging.Logger;

import com.example.model.ChatRequest;
import com.example.model.ChatResponse;
import com.example.service.RagEvaluationService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatbotResource {

    private static final Logger LOG = Logger.getLogger(ChatbotResource.class);

    @Inject
    RagEvaluationService evaluationService;

    @POST
    public ChatResponse chat(ChatRequest request) {
        LOG.infof("Received chat request: %s", request.question);
        return evaluationService.processAndEvaluate(request);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "Banking Chatbot with Evaluation is running!";
    }
}
package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
public class ChatbotResource {

    @Inject
    BankingChatbot bankingChatbot;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(String question) {
        return new ChatResponse(bankingChatbot.chat("sessionId", question));
    }

    public record ChatResponse(String answer) {
    }
}
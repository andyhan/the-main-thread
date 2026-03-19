package com.acme.docling;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/bot")
public class SalesEnablementResource {

    @Inject
    SalesEnablementBot bot;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BotResponse ask(@QueryParam("q") String question) {
        if (question == null || question.isBlank()) {
            question = "What are the enterprise tier features of CloudX?";
        }
        return new BotResponse(bot.chat(question));
    }
}

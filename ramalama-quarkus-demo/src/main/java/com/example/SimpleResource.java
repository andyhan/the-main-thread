package com.example;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/simple")
public class SimpleResource {

    @Inject
    ChatModel chatLanguageModel;

    @GET
    @Path("/{country}")
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(@PathParam("country") String country) {

        String prompt = """
                What's the capital of %s?
                Describe the history of that city briefly.
                """.formatted(country);

        return chatLanguageModel.chat(prompt);
    }
}
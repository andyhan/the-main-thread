package com.themainthread.iss.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "iss-api")
@Path("/iss-now.json")
public interface IssApiClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    IssNowResponse fetchPosition();
}
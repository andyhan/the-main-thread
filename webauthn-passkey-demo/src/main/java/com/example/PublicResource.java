package com.example;

import java.security.Principal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Public endpoints accessible without authentication.
 */
@Path("/api/public")
public class PublicResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String publicEndpoint() {
        return "This is a public endpoint - no authentication required!";
    }

    @GET
    @Path("/me")
    @Produces(MediaType.TEXT_PLAIN)
    public String currentUser(@Context SecurityContext securityContext) {
        Principal user = securityContext.getUserPrincipal();
        return user != null ? user.getName() : "<not logged in>";
    }
}
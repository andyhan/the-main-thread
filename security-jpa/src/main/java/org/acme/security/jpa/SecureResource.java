package org.acme.security.jpa;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api")
public class SecureResource {

    @Inject
    CurrentUserService currentUserService;

    @GET
    @Path("/public")
    public String publicEndpoint() {
        return "This is public.";
    }

    @GET
    @Path("/user")
    @RolesAllowed("user")
    public String userEndpoint() {
        User user = currentUserService.getCurrentUser();
        String name = user != null ? user.getDisplayNameOrUsername() : "user";
        return "Hello, " + name + "!";
    }

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String adminEndpoint() {
        return "Hello, admin!";
    }
}

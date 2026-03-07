package com.example.auth;

import java.net.URI;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/oidc/callback")
@RequestScoped
public class OidcCallbackResource {

    @Inject
    OidcUserSyncService oidcUserSyncService;

    @GET
    @Authenticated
    public Response callback() {
        User user = oidcUserSyncService.syncFromOidc();
        return Response.seeOther(URI.create("/welcome?email=" + user.email)).build();
    }
}
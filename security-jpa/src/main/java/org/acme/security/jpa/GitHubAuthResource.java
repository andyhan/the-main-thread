package org.acme.security.jpa;

import io.quarkus.oidc.AuthorizationCodeFlow;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Entry point for "Sign in with GitHub". This path is protected by OIDC authorization code flow:
 * unauthenticated users are redirected to GitHub; after callback they are redirected to home.
 */
@Path("/github")
public class GitHubAuthResource {

    @GET
    @AuthorizationCodeFlow
    public Response github() {
        return Response.seeOther(UriBuilder.fromPath("/").build()).build();
    }
}

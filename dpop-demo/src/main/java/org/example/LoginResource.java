package org.example;

import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.security.TokenService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/login")
public class LoginResource {

    @ConfigProperty(name = "dpop.login.default-subject", defaultValue = "alice")
    String defaultSubject;

    TokenService tokenService;

    public LoginResource(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Issues an access token for the given subject.
     * In a real application you would validate credentials (e.g. password or client
     * secret)
     * and derive subject and groups from your identity store.
     * The returned JWT is used as the access token in the Authorization header with
     * DPoP.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.WILDCARD })
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        String subject = (request != null && request.subject() != null) ? request.subject() : defaultSubject;
        Set<String> groups = (request != null && request.groups() != null && !request.groups().isEmpty())
                ? request.groups()
                : Set.of("account-viewer");

        String accessToken = tokenService.createAccessToken(subject, groups);
        return Response.ok(new LoginResponse(accessToken)).build();
    }

    public record LoginRequest(String subject, Set<String> groups) {
    }

    public record LoginResponse(String accessToken) {
    }
}
package org.acme.security.jpa;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Path("/logout")
public class LogoutResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    OidcSession oidcSession;

    private static Response redirectToLogin() {
        return Response.seeOther(UriBuilder.fromPath("/login").build()).build();
    }

    @POST
    public Uni<Response> logout() {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(redirectToLogin());
        }
        try {
            FormAuthenticationMechanism.logout(identity);
            return Uni.createFrom().item(redirectToLogin());
        } catch (Exception e) {
            return oidcSession.logout().replaceWith(redirectToLogin());
        }
    }
}
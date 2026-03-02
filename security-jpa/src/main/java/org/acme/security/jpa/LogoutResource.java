package org.acme.security.jpa;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcSession;
import io.quarkus.security.identity.CurrentIdentityAssociation;
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
    CurrentIdentityAssociation currentIdentityAssociation;

    @Inject
    OidcSession oidcSession;

    private static Response redirectToLogin() {
        return Response.seeOther(UriBuilder.fromPath("/login").build()).build();
    }

    @POST
    public Uni<Response> logout() {
        return currentIdentityAssociation.getDeferredIdentity()
                .flatMap(identity -> {
                    if (identity.isAnonymous()) {
                        return Uni.createFrom().item(redirectToLogin());
                    }
                    if (identity.getCredential(AccessTokenCredential.class) != null) {
                        return oidcSession.logout().replaceWith(redirectToLogin());
                    }
                    FormAuthenticationMechanism.logout(identity);
                    return Uni.createFrom().item(redirectToLogin());
                });
    }
}
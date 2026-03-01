package org.acme.security.jpa;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Assigns a default "user" role to authenticated identities that have no roles.
 * Form-login users get roles from the {@link User} entity (via Quarkus Security JPA).
 * OIDC users (e.g. GitHub) typically have no application roles in the token, so we add
 * "user" so they can access @RolesAllowed("user") endpoints.
 * <p>
 * For a full application you might instead look up the user by OIDC subject or email in your
 * DB and add roles from there (e.g. first login: create User row with role "user"; admins
 * assign "admin" in the DB; this augmentor would then load roles from the User entity).
 */
@ApplicationScoped
public class OidcRolesAugmentor implements SecurityIdentityAugmentor {

    private static final String DEFAULT_OIDC_ROLE = "user";

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        return Uni.createFrom().item(build(identity));
    }

    private SecurityIdentity build(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            return identity;
        }
        if (!identity.getRoles().isEmpty()) {
            return identity;
        }
        return QuarkusSecurityIdentity.builder(identity).addRole(DEFAULT_OIDC_ROLE).build();
    }
}

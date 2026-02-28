package org.example.security;

import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "dpop.jwt.issuer", defaultValue = "dpop-demo")
    String issuer;

    /**
     * Builds a signed JWT access token for the given subject and groups.
     * The token can be used with DPoP when calling protected endpoints.
     * Expiry is set via smallrye.jwt.new-token.lifespan (default 300 seconds).
     */
    public String createAccessToken(String subject, Set<String> groups) {
        return Jwt.upn(subject)
                .subject(subject)
                .groups(groups)
                .issuer(issuer)
                .sign();
    }
}
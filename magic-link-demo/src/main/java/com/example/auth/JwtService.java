package com.example.auth;

import java.time.Duration;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtService {

    public String generateSessionToken(User user) {
        return Jwt.issuer("https://example.local")
                .upn(user.email)
                .subject(String.valueOf(user.id))
                .groups(Set.of("user"))
                .claim("displayName", user.displayName)
                .claim("emailVerified", user.emailVerified)
                .expiresIn(Duration.ofHours(8))
                .sign();
    }
}
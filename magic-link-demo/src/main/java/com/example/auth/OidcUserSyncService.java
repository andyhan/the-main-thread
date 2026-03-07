package com.example.auth;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OidcUserSyncService {

    @Inject
    JsonWebToken idToken;

    @Transactional
    public User syncFromOidc() {
        String email = idToken.getClaim("email");
        String name = idToken.getClaim("name");

        return User.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.email = email.toLowerCase();
            user.displayName = name != null ? name : email;
            user.passwordHash = "";
            user.emailVerified = true;
            user.persist();
            return user;
        });
    }
}
package com.example.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    @ConfigProperty(name = "app.magic-link.expiry-minutes", defaultValue = "15")
    int magicLinkExpiryMinutes;

    @ConfigProperty(name = "app.email-verify.expiry-hours", defaultValue = "48")
    int emailVerifyExpiryHours;

    @ConfigProperty(name = "app.password-reset.expiry-minutes", defaultValue = "20")
    int passwordResetExpiryMinutes;

    @Transactional
    public String createToken(User user, TokenPurpose purpose) {
        AuthToken.invalidateExisting(user, purpose);

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);

        AuthToken authToken = new AuthToken();
        authToken.user = user;
        authToken.tokenHash = sha256Hex(rawToken);
        authToken.purpose = purpose;
        authToken.expiresAt = Instant.now().plusSeconds(resolveLifetimeSeconds(purpose));
        authToken.persist();

        return rawToken;
    }

    @Transactional
    public Optional<User> validateAndConsume(String rawToken, TokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String hash = sha256Hex(rawToken.trim());

        return AuthToken.findValid(hash, purpose).map(token -> {
            token.used = true;
            token.usedAt = Instant.now();
            return token.user;
        });
    }

    private long resolveLifetimeSeconds(TokenPurpose purpose) {
        return switch (purpose) {
            case MAGIC_LOGIN -> magicLinkExpiryMinutes * 60L;
            case EMAIL_VERIFY -> emailVerifyExpiryHours * 3600L;
            case PASSWORD_RESET -> passwordResetExpiryMinutes * 60L;
        };
    }

    String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
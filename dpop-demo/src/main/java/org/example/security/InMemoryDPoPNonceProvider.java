package org.example.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.quarkus.oidc.DPoPNonceProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryDPoPNonceProvider implements DPoPNonceProvider {

    private static final Logger LOG = Logger.getLogger(InMemoryDPoPNonceProvider.class);

    private static final long NONCE_TTL_SECONDS = 30;

    private final ConcurrentHashMap<String, Instant> store = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String getNonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String nonce = encoder.encodeToString(bytes);

        store.put(nonce, Instant.now().plusSeconds(NONCE_TTL_SECONDS));
        LOG.debugf("Issued nonce %s", nonce);

        return nonce;
    }

    @Override
    public boolean isValid(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }

        Instant expiry = store.remove(nonce);

        if (expiry == null) {
            return false;
        }

        if (Instant.now().isAfter(expiry)) {
            return false;
        }

        return true;
    }
}
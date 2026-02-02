package com.example.encryption;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EncryptionService {

    @Inject
    VaultKVSecretEngine kvEngine;

    @Inject
    VaultTransitSecretEngine transitEngine;

    private static final String TRANSIT_KEY_ID = "patient-data";
    private static final String KV_KEY_NAME = "master-key";
    private static final String KV_SECRET_PATH = "encryption";

    private final Map<String, CachedKey> cache = new ConcurrentHashMap<>();
    private static final long TTL = 3600000; // 1 hour

    /**
     * Encrypts plaintext using the Vault Transit Engine.
     * Vault handles the base64 encoding internally.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Call Vault Transit directly with plaintext
            // Vault will handle encoding internally
            return transitEngine.encrypt(TRANSIT_KEY_ID, plaintext);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed via Vault Transit", e);
        }
    }

    /**
     * Decrypts ciphertext using the Vault Transit Engine.
     * Vault returns the original plaintext.
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        try { // Send to Vault for decryption (returns ClearData)
            return transitEngine.decrypt(TRANSIT_KEY_ID, ciphertext).asString();

        } catch (Exception e) {
            throw new EncryptionException("Decryption failed via Vault Transit", e);
        }
    }

    /**
     * Creates a deterministic, keyed hash for searchable fields.
     */
    public String hashForSearch(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            byte[] key = getOrFetchPepperKey();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key);
            digest.update(plaintext.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (Exception e) {
            throw new EncryptionException("Failed to hash for search", e);
        }
    }

    private byte[] getOrFetchPepperKey() {
        CachedKey cached = cache.get(KV_KEY_NAME);
        if (cached != null && !cached.isExpired()) {
            return cached.key;
        }

        String base64 = kvEngine.readSecret(KV_SECRET_PATH)
                .get(KV_KEY_NAME)
                .toString();

        byte[] key = Base64.getDecoder().decode(base64);
        cache.put(KV_KEY_NAME, new CachedKey(key, System.currentTimeMillis()));

        return key;
    }

    private static class CachedKey {
        final byte[] key;
        final long timestamp;

        CachedKey(byte[] key, long timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TTL;
        }
    }
}
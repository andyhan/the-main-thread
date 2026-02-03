package com.example;

import java.util.List;
import java.util.Set;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@Blocking
@ApplicationScoped
public class WebAuthnSetup implements WebAuthnUserProvider {

    @Override
    @Transactional
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String username) {
        return Uni.createFrom().item(
                WebAuthnCredential.<WebAuthnCredential>list("user.username", username)
                        .stream()
                        .map(WebAuthnCredential::toRecord)
                        .toList());
    }

    @Override
    @Transactional
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        WebAuthnCredential credential = WebAuthnCredential.findById(credentialId);
        if (credential == null) {
            return Uni.createFrom().failure(new RuntimeException("Credential not found"));
        }
        return Uni.createFrom().item(credential.toRecord());
    }

    @Override
    @Transactional
    public Uni<Void> store(WebAuthnCredentialRecord record) {
        if (User.findByUsername(record.getUsername()) != null) {
            return Uni.createFrom().failure(
                    new RuntimeException("User already exists"));
        }

        User user = new User();
        user.username = record.getUsername();

        WebAuthnCredential credential = new WebAuthnCredential(record, user);
        user.persist();
        credential.persist();

        return Uni.createFrom().voidItem();
    }

    @Override
    @Transactional
    public Uni<Void> update(String credentialId, long counter) {
        WebAuthnCredential credential = WebAuthnCredential.findById(credentialId);
        if (credential != null) {
            credential.counter = counter;
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    public Set<String> getRoles(String username) {
        if ("admin".equals(username)) {
            return Set.of("user", "admin");
        }
        return Set.of("user");
    }
}
package com.example;

import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class WebAuthnCredential extends PanacheEntityBase {

    @Id
    public String credentialId;

    public byte[] publicKey;
    public long publicKeyAlgorithm;
    public long counter;
    public UUID aaguid;

    @OneToOne
    public User user;

    public WebAuthnCredential() {
    }

    public WebAuthnCredential(WebAuthnCredentialRecord record, User user) {
        RequiredPersistedData data = record.getRequiredPersistedData();
        this.credentialId = data.credentialId();
        this.publicKey = data.publicKey();
        this.publicKeyAlgorithm = data.publicKeyAlgorithm();
        this.counter = data.counter();
        this.aaguid = data.aaguid();
        this.user = user;
        user.webAuthnCredential = this;
    }

    public WebAuthnCredentialRecord toRecord() {
        return WebAuthnCredentialRecord.fromRequiredPersistedData(
                new RequiredPersistedData(
                        user.username,
                        credentialId,
                        aaguid,
                        publicKey,
                        publicKeyAlgorithm,
                        counter));
    }
}
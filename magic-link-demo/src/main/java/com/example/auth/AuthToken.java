package com.example.auth;

import java.time.Instant;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_token", indexes = {
        @Index(name = "idx_auth_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_auth_token_expires", columnList = "expiresAt")
})
public class AuthToken extends PanacheEntity {

    @ManyToOne(optional = false)
    public User user;

    @Column(nullable = false, unique = true, length = 64)
    public String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    public TokenPurpose purpose;

    @Column(nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean used = false;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    @Column
    public Instant usedAt;

    public static Optional<AuthToken> findValid(String tokenHash, TokenPurpose purpose) {
        return find(
                "tokenHash = ?1 and purpose = ?2 and used = false and expiresAt > ?3",
                tokenHash,
                purpose,
                Instant.now()).firstResultOptional();
    }

    public static long invalidateExisting(User user, TokenPurpose purpose) {
        return update(
                "used = true, usedAt = ?1 where user = ?2 and purpose = ?3 and used = false",
                Instant.now(),
                user,
                purpose);
    }

    public static long deleteExpiredOrUsed() {
        return delete("expiresAt < ?1 or used = true", Instant.now());
    }
}
package com.example.auth;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 320)
    public String email;

    @Column(nullable = false, length = 120)
    public String displayName;

    @Column(nullable = false, length = 100)
    public String passwordHash;

    @Column(nullable = false)
    public boolean emailVerified = false;

    public static Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return find("email", email.trim().toLowerCase()).firstResultOptional();
    }
}
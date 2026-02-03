package com.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_table")
public class User extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String username;

    public String firstName;
    public String lastName;

    @OneToOne(mappedBy = "user")
    public WebAuthnCredential webAuthnCredential;

    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }
}
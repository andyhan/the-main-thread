package com.example.entity;

import java.time.LocalDate;

import com.example.encryption.EncryptedStringConverter;
import com.example.encryption.SearchableEncryptedConverter;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "patients", indexes = {
        // Index on email for fast searching by hash prefix
        @Index(name = "idx_email_hash", columnList = "email")
})
public class Patient extends PanacheEntity {

    // Plain text fields
    @Column(nullable = false)
    public String firstName;

    @Column(nullable = false)
    public String lastName;

    @Column(nullable = false)
    public LocalDate dateOfBirth;

    @Column(nullable = false)
    public String phoneNumber;

    // Searchable encrypted field (stores HASH:CIPHERTEXT)
    @Convert(converter = SearchableEncryptedConverter.class)
    @Column(nullable = false, unique = true, length = 1000)
    public String email;

    // Fully encrypted fields (stores only CIPHERTEXT)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 1000)
    public String ssn;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 5000)
    public String medicalHistory;
}
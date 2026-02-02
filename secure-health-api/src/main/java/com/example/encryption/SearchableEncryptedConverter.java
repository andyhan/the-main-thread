package com.example.encryption;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter that stores both a hash and encrypted value.
 * Format in DB: HASH:CIPHERTEXT
 * This allows searching on the hash while keeping data encrypted.
 * Example: Email addresses
 */
@Converter(autoApply = false)
@ApplicationScoped
public class SearchableEncryptedConverter implements AttributeConverter<String, String> {

    @Inject
    EncryptionService encryption;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        // Create a searchable hash
        String hash = encryption.hashForSearch(attribute);

        // Encrypt the actual value
        String encrypted = encryption.encrypt(attribute);

        // Store as: HASH:CIPHERTEXT
        return hash + ":" + encrypted;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        // Find the separator
        int colonIndex = dbData.indexOf(':');

        // Handle legacy data or malformed entries
        if (colonIndex == -1) {
            return encryption.decrypt(dbData);
        }

        // Extract and decrypt only the ciphertext portion
        String encrypted = dbData.substring(colonIndex + 1);
        return encryption.decrypt(encrypted);
    }
}
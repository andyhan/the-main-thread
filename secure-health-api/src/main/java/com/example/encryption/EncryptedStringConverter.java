package com.example.encryption;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter that automatically encrypts/decrypts String fields.
 * Use this for fields that need encryption but don't need to be searchable.
 * Example: SSN, medical history
 */
@Converter(autoApply = false)
@ApplicationScoped
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Inject
    EncryptionService encryption;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Called when saving to database
        return encryption.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Called when reading from database
        return encryption.decrypt(dbData);
    }
}
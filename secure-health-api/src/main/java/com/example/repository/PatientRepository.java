package com.example.repository;

import java.util.List;
import java.util.Optional;

import com.example.encryption.EncryptionService;
import com.example.entity.Patient;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PatientRepository implements PanacheRepository<Patient> {

    @Inject
    EncryptionService encryption;

    /**
     * Find patient by email address.
     * Searches using the hash prefix stored in the database (raw column value).
     * Uses a native query so the parameter is bound as-is without going through
     * the SearchableEncryptedConverter, which would misinterpret the search
     * pattern.
     */
    @SuppressWarnings("unchecked")
    public Optional<Patient> findByEmail(String email) {
        String hash = encryption.hashForSearch(email);
        // Pattern must match stored format: HASH:CIPHERTEXT
        String pattern = hash + ":%";

        EntityManager em = Panache.getEntityManager();
        List<Patient> list = em.createNativeQuery(
                "SELECT * FROM patients WHERE email LIKE ?1", Patient.class)
                .setParameter(1, pattern)
                .getResultList();

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Check if an email already exists in the database.
     */
    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }
}
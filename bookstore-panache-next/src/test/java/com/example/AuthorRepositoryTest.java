package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuthorRepositoryTest {

    @Inject
    Author.Repo repo;

    @Test
    @TestTransaction // rolls back after each test
    void shouldPersistAndFindByName() {
        Author a = new Author();
        a.name = "Octavia Butler";
        a.country = "USA";
        repo.persist(a);

        Optional<Author> found = repo.findByName("Octavia Butler");
        assertTrue(found.isPresent());
        assertEquals("USA", found.get().country);
    }

    @Test
    @TestTransaction
    void shouldCountByCountry() {
        for (int i = 0; i < 3; i++) {
            Author a = new Author();
            a.name = "Author " + i;
            a.country = "UK";
            repo.persist(a);
        }
        assertEquals(3, repo.findByCountry("UK").size());
    }
}

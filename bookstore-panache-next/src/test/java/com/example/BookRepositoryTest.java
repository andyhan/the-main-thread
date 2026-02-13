package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;

@QuarkusTest
class BookRepositoryTest {

    @Inject
    Book.Repo repo;

    @Test
    @RunOnVertxContext
    void shouldInsertAndFindBook(UniAsserter asserter) {
        var book = new Book();
        book.title = "Kindred";
        book.year = 1979;

        asserter.assertThat(
                () -> SessionOperations.withStatelessTransaction(() -> repo.insert(book)),
                v -> { /* insert completed */ });

        asserter.assertThat(
                () -> SessionOperations.withStatelessTransaction(() -> repo.findByTitle("Kindred")),
                list -> {
                    assertFalse(list.isEmpty());
                    assertEquals("Kindred", list.get(0).title);
                    assertEquals(1979, list.get(0).year);
                });
    }
}

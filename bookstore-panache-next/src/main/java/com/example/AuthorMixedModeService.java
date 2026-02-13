package com.example;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class AuthorMixedModeService {

    @Inject
    Author.WriteRepo writeRepo;
    @Inject
    Author.ReadRepo readRepo;

    // Blocking managed — dirty check, no explicit update()
    @Transactional
    public Author createAuthor(String name, String country) {
        Author a = new Author();
        a.name = name;
        a.country = country;
        writeRepo.persist(a);
        return a;
    }

    @Transactional
    public void renameAuthor(Long id, String newName) {
        Author a = writeRepo.findById(id);
        if (a == null)
            throw new NotFoundException();
        a.name = newName;
    }

    // Blocking stateless — explicit insert/update/delete (no @Transactional: stateless uses its own session)
    public void bulkImport(List<Author> authors) {
        authors.forEach(a -> a.statelessBlocking().insert());
    }

    public void bulkUpdateCountry(List<Author> authors, String country) {
        authors.forEach(a -> {
            a.country = country;
            a.statelessBlocking().update();
        });
    }

    // Reactive managed — non-blocking, dirty check
    @WithTransaction
    public Uni<Author> createAuthorReactive(String name, String country) {
        Author a = new Author();
        a.name = name;
        a.country = country;
        return a.managedReactive().persist().replaceWith(a);
    }

    @WithTransaction
    public Uni<Void> renameAuthorReactive(Long id, String newName) {
        Author a = writeRepo.findById(id);
        if (a == null)
            return Uni.createFrom().failure(new NotFoundException());
        a.name = newName;
        return Uni.createFrom().voidItem();
    }

    // Reactive stateless — needs stateless session (do not use @WithTransaction)
    public Uni<List<Author>> getCatalogByCountry(String country) {
        return SessionOperations.withStatelessTransaction(() -> readRepo.catalog(country));
    }

    public Uni<Void> bulkImportReactive(List<Author> authors) {
        return SessionOperations.withStatelessTransaction(() ->
                Uni.join()
                        .all(authors.stream()
                                .map(a -> a.statelessReactive().insert())
                                .toList())
                        .andFailFast()
                        .replaceWithVoid());
    }
}
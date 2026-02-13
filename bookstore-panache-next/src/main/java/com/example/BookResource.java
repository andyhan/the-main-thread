package com.example;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    @Inject
    Book.Repo repo;

    @GET
    public Uni<List<Book>> list() {
        return SessionOperations.withStatelessTransaction(() -> repo.findAll().list());
    }

    @GET
    @Path("/since/{year}")
    public Uni<List<Book>> since(@PathParam("year") int year) {
        return SessionOperations.withStatelessTransaction(() -> repo.findPublishedSince(year));
    }

    @POST
    public Uni<Response> create(Book book) {
        return SessionOperations.withStatelessTransaction(
                () -> repo.insert(book).map(b -> Response.status(201).entity(b).build()));
    }

    @PUT
    @Path("/{id}")
    public Uni<Book> update(@PathParam("id") Long id, Book patch) {
        return SessionOperations.withStatelessTransaction(() -> {
            Book existing = repo.findById(id);
            if (existing == null)
                return Uni.createFrom().failure(new NotFoundException());
            existing.title = patch.title;
            existing.year = patch.year;
            return repo.update(existing).replaceWith(existing);
        });
    }

    @DELETE
    @Path("/older-than/{year}")
    public Uni<Integer> cleanup(@PathParam("year") int year) {
        return SessionOperations.withStatelessTransaction(() -> repo.deleteOlderThan(year));
    }
}

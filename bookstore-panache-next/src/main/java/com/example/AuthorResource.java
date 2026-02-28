package com.example;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/authors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthorResource {

    // Inject via the nested interface type
    @Inject
    Author.Repo repo;

    @GET
    public List<Author> list() {
        return repo.listAll();
    }

    @GET
    @Path("/country/{country}")
    public List<Author> byCountry(@PathParam("country") String country) {
        return repo.findByCountry(country);
    }

    @POST
    @Transactional
    public Response create(Author author) {
        repo.persist(author);
        return Response.status(201).entity(author).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Author update(@PathParam("id") Long id, Author patch) {
        Author existing = repo.findById(id);
        if (existing == null)
            throw new NotFoundException();
        existing.name = patch.name;
        existing.country = patch.country;
        // Managed session: no explicit update() — dirty check handles it
        return existing;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        repo.deleteById(id);
    }
}

package dev.example.catalog;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/books")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed("librarian")
    public Response listAll() {
        String json = String.format(
                "{\"subject\":\"%s\"}",
                jwt.getSubject());
        return Response.ok(json).build();
    }

    @POST
    @RolesAllowed({ "librarian", "editor" })
    public Response addBook(String body) {
        return Response.status(201)
                .entity("{\"status\":\"created\"}")
                .build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("librarian")
    public Response deleteBook(@PathParam("id") String id) {
        return Response.noContent().build();
    }
}
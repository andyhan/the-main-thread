package dev.example.catalog;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {

    @Inject
    CatalogConfig config;

    @GET
    @Path("/config-info")
    public Response configInfo() {
        String json = String.format(
                "{\"maxResults\":%d,\"backend\":\"%s\",\"recommendations\":%b}",
                config.getMaxResults(),
                config.getStorageBackend(),
                config.isRecommendationsEnabled());
        return Response.ok(json).build();
    }

    @Inject
    RecommendationService recommendations;

    @GET
    @Path("/{id}/recommendations")
    public Response getRecommendations(@PathParam("id") String id) {
        return Response.ok(
                recommendations.getRecommendations(id)).build();
    }
}
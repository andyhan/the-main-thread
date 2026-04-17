package com.themainthread.cache;

import java.math.BigDecimal;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import com.themainthread.cache.service.CatalogUpdateService;
import com.themainthread.cache.service.PriceLookupService;

import io.smallrye.mutiny.Uni;

@Path("/prices")
public class CacheResource {

    @Inject
    PriceLookupService priceLookup;

    @Inject
    CatalogUpdateService catalogUpdate;

    @GET
    @Path("/{sku}")
    public Uni<Response> getPrice(@PathParam("sku") String sku) {
        return priceLookup.lookupPrice(sku)
                .onItem().transform(price -> Response.ok(price).build());
    }

    @PUT
    @Path("/{sku}")
    public Response updatePrice(@PathParam("sku") String sku,
            @QueryParam("price") BigDecimal price,
            @QueryParam("fail") boolean fail) {
        BigDecimal newPrice = price != null ? price : new BigDecimal("19.99");
        try {
            catalogUpdate.updatePrice(sku, newPrice, fail);
            return Response.ok().build();
        } catch (RuntimeException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{sku}/force-invalidate")
    public Response updatePriceForced(@PathParam("sku") String sku,
            @QueryParam("price") BigDecimal price,
            @QueryParam("fail") boolean fail) {
        BigDecimal newPrice = price != null ? price : new BigDecimal("19.99");
        try {
            catalogUpdate.updatePriceWithForcedInvalidation(sku, newPrice, fail);
            return Response.ok().build();
        } catch (RuntimeException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
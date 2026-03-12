package dev.mainthread;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/shipments")
@Produces(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    @Inject
    ShipmentService shipmentService;

    @GET
    @Path("/{trackingNumber}")
    public ShipmentStatus track(@PathParam("trackingNumber") String trackingNumber) {
        return shipmentService.getStatus(trackingNumber);
    }
}
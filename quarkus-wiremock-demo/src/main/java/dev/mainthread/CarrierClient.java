package dev.mainthread;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import dev.mainthread.dto.CarrierTrackingResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "carrier-api")
@Path("/v1/track")
public interface CarrierClient {

    @GET
    @Path("/{trackingNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    CarrierTrackingResponse track(@PathParam("trackingNumber") String trackingNumber);
}
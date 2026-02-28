package org.example;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/accounts")
@Authenticated
public class BankAccountResource {

    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("/{accountId}/balance")
    @RolesAllowed("account-viewer")
    @Produces(MediaType.APPLICATION_JSON)
    public BalanceResponse getBalance(@PathParam("accountId") String accountId) {
        return new BalanceResponse(
                accountId,
                accessToken.getSubject(),
                1_234_567);
    }

    public record BalanceResponse(String accountId, String owner, long balancePence) {
    }
}
package org.example;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
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
    SecurityIdentity identity;

    @GET
    @Path("/{accountId}/balance")
    @RolesAllowed("account-viewer")
    @Produces(MediaType.APPLICATION_JSON)
    public BalanceResponse getBalance(@PathParam("accountId") String accountId) {
        String subject = identity.getPrincipal() != null ? identity.getPrincipal().getName() : null;
        return new BalanceResponse(
                accountId,
                subject,
                1_234_567);
    }

    public record BalanceResponse(String accountId, String owner, long balancePence) {
    }
}
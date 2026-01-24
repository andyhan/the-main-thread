package com.mainthread.problem;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import com.mainthread.problem.errors.ErrorRegistry;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/bank")
@Produces(MediaType.APPLICATION_JSON)
public class BankResource {

    @GET
    @Path("/withdraw/{amount}")
    @APIResponse(
            responseCode = "400",
            description = "Bad Request - Invalid withdrawal amount or insufficient funds",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = HttpProblem.class)
            )
    )
    public String withdraw(@PathParam("amount") int amount) {

        if (amount > 1000) {
            throw HttpProblem.builder()
                    .withType(ErrorRegistry.INSUFFICIENT_FUNDS.getType())
                    .withTitle(ErrorRegistry.INSUFFICIENT_FUNDS.getTitle())
                    .withStatus(Response.Status.BAD_REQUEST)
                    .withDetail("Current balance is 500, but you requested " + amount)
                    .with("current_balance", 500)
                    .with("requested_amount", amount)
                    .build();
        }

        if (amount < 0) {
            throw new IllegalArgumentException("Withdrawal amount cannot be negative");
        }

        return "Withdrawal successful";
    }

    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "400", description = "Invalid ID format", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = HttpProblem.class)))
    @APIResponse(responseCode = "404", description = "Account not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = HttpProblem.class)))
    public String get(@PathParam("id") Long id) {
        return "ok";
    }
}
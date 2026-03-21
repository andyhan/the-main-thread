package org.acme;

import org.acme.domain.BollingerSignal;
import org.acme.service.BollingerService;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class DashboardResource {

    @Inject
    Template index;

    @Inject
    BollingerService service;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return index.data("title", "BTC Bollinger Bands");
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<BollingerSignal> stream() {
        return service.stream();
    }
}
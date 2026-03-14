package com.themainthread.iss.resource;

import org.jboss.resteasy.reactive.RestStreamElementType;

import com.themainthread.iss.service.IssPositionCache;
import com.themainthread.iss.service.IssPositionCache.PositionFix;

import java.util.List;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/iss")
public class IssResource {

    @Inject
    IssPositionCache cache;

    @GET
    @Path("/position")
    @Produces(MediaType.APPLICATION_JSON)
    public PositionFix position() {
        return cache.latest();
    }

    @GET
    @Path("/positions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PositionFix> positions() {
        return cache.path();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<PositionFix> stream() {
        return cache.stream();
    }
}
package com.demo.workout;

import java.io.InputStream;
import java.util.Arrays;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.demo.gpx.GpxService;
import com.demo.render.TrackImageService;
import com.demo.render.TrackImageService.Point;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/workouts")
public class WorkoutResource {

    @Inject
    GpxService gpxService;

    @Inject
    TrackImageService trackImageService;

    @Inject
    WorkoutMapper mapper;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public WorkoutDto upload(@RestForm("file") FileUpload file) throws Exception {
        try (InputStream in = file.uploadedFile().toFile().toURI().toURL().openStream()) {
            Workout workout = gpxService.parseAndSimplify(in);
            workout.persist();
            return mapper.toDto(workout);
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public WorkoutDto get(@PathParam("id") long id) {
        Workout workout = Workout.findById(id);
        if (workout == null) {
            throw new NotFoundException();
        }
        return mapper.toDto(workout);
    }

    @GET
    @Path("/{id}/image")
    @Produces("image/png")
    @Transactional
    public Response image(@PathParam("id") long id) throws Exception {
        Workout workout = Workout.findById(id);
        if (workout == null) {
            return Response.status(404).build();
        }

        if (workout.route == null) {
            return Response.status(404).entity("Workout route not found").build();
        }

        var coords = workout.route.getCoordinates();
        if (coords == null || coords.length < 2) {
            return Response.status(400).entity("Insufficient coordinates in route").build();
        }

        var points = Arrays.stream(coords)
                .map(c -> new Point(c.y, c.x)) // JTS: x=lon, y=lat
                .toList();

        byte[] png = trackImageService.renderTrackToPng(points);

        return Response.ok(png)
                .header("Content-Disposition", "inline; filename=\"track.png\"")
                .build();
    }
}
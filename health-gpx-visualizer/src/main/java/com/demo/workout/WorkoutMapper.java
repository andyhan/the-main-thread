package com.demo.workout;

import org.locationtech.jts.io.geojson.GeoJsonWriter;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkoutMapper {

    private final GeoJsonWriter writer = new GeoJsonWriter();

    public WorkoutDto toDto(Workout w) {
        String geoJson = (w.route == null) ? null : writer.write(w.route);
        return new WorkoutDto(
                w.id,
                w.name,
                w.startTime,
                w.totalDistanceMeters,
                w.avgHeartRate,
                w.maxHeartRate,
                geoJson);
    }
}
package com.demo.workout;

import java.time.LocalDateTime;

public record WorkoutDto(
        Long id,
        String name,
        LocalDateTime startTime,
        double totalDistanceMeters,
        int avgHeartRate,
        int maxHeartRate,
        String routeGeoJson) {
}
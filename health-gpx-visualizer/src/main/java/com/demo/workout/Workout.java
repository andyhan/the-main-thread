package com.demo.workout;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.LineString;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Workout extends PanacheEntity {

    public String name;

    public LocalDateTime startTime;

    public double totalDistanceMeters;

    public int avgHeartRate;

    public int maxHeartRate;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    public LineString route;
}
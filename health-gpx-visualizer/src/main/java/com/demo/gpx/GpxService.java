package com.demo.gpx;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.demo.workout.Workout;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Metadata;
import io.jenetics.jpx.WayPoint;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GpxService {

    // Common TrackPointExtension namespaces seen in the wild.
    // Apple Health exports often reference the Garmin gpxtpx schema.
    private static final String GPXTPX_V1 = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";
    private static final String GPXTPX_V2 = "http://www.garmin.com/xmlschemas/TrackPointExtension/v2";

    @Inject
    GeometryFactory geometryFactory;

    public Workout parseAndSimplify(InputStream content) throws Exception {
        GPX gpx = GPX.Reader.of(GPX.Reader.Mode.LENIENT).read(content);

        List<WayPoint> points = gpx.tracks()
                .flatMap(t -> t.segments())
                .flatMap(s -> s.points())
                .toList();

        if (points.isEmpty()) {
            throw new IllegalArgumentException("No track points found in GPX");
        }

        List<Integer> heartRates = new ArrayList<>();
        for (WayPoint p : points) {
            extractHeartRate(p).ifPresent(heartRates::add);
        }

        int avgHr = heartRates.isEmpty() ? 0
                : (int) Math.round(heartRates.stream().mapToInt(i -> i).average().orElse(0));
        int maxHr = heartRates.isEmpty() ? 0 : heartRates.stream().mapToInt(i -> i).max().orElse(0);

        Coordinate[] coords = points.stream()
                .map(p -> new Coordinate(p.getLongitude().doubleValue(), p.getLatitude().doubleValue()))
                .toArray(Coordinate[]::new);

        LineString raw = geometryFactory.createLineString(coords);

        // Roughly ~11m at the equator. Good enough for “share images” and avoids
        // overplotting.
        LineString simplified = (LineString) DouglasPeuckerSimplifier.simplify(raw, 0.0001);

        Workout w = new Workout();
        w.name = gpx.getMetadata()
                .flatMap(Metadata::getName)
                .orElse("Untitled Workout");
        w.startTime = extractStart(points).orElse(null);
        w.route = simplified;
        w.avgHeartRate = avgHr;
        w.maxHeartRate = maxHr;
        w.totalDistanceMeters = computeHaversineMeters(points);

        return w;
    }

    private Optional<LocalDateTime> extractStart(List<WayPoint> points) {
        return points.getFirst().getTime()
                .map(t -> LocalDateTime.ofInstant(t, ZoneId.systemDefault()));
    }

    private double computeHaversineMeters(List<WayPoint> points) {
        double total = 0.0;

        for (int i = 1; i < points.size(); i++) {
            WayPoint a = points.get(i - 1);
            WayPoint b = points.get(i);

            double lat1 = Math.toRadians(a.getLatitude().doubleValue());
            double lon1 = Math.toRadians(a.getLongitude().doubleValue());
            double lat2 = Math.toRadians(b.getLatitude().doubleValue());
            double lon2 = Math.toRadians(b.getLongitude().doubleValue());

            double dLat = lat2 - lat1;
            double dLon = lon2 - lon1;

            double sinLat = Math.sin(dLat / 2.0);
            double sinLon = Math.sin(dLon / 2.0);

            double h = sinLat * sinLat
                    + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;

            double c = 2.0 * Math.atan2(Math.sqrt(h), Math.sqrt(1.0 - h));

            // Earth radius in meters
            total += 6_371_000.0 * c;
        }

        return total;
    }

    private Optional<Integer> extractHeartRate(WayPoint p) {
        // jpx exposes extensions as Optional<Document>
        return p.getExtensions()
                .map(doc -> extractHrFromDocument(doc))
                .orElse(Optional.empty());
    }

    private Optional<Integer> extractHrFromDocument(Document doc) {
        // First try known Garmin TrackPointExtension namespaces.
        Optional<Integer> v2 = firstInt(doc.getElementsByTagNameNS(GPXTPX_V2, "hr"));
        if (v2.isPresent()) {
            return v2;
        }

        Optional<Integer> v1 = firstInt(doc.getElementsByTagNameNS(GPXTPX_V1, "hr"));
        if (v1.isPresent()) {
            return v1;
        }

        // Last-resort fallback: some exporters mess up namespaces or strip prefixes.
        // This keeps the tutorial resilient without pretending GPX is always clean.
        NodeList anyHr = doc.getElementsByTagNameNS("*", "hr");
        return firstInt(anyHr);
    }

    private Optional<Integer> firstInt(NodeList nodes) {
        if (nodes == null || nodes.getLength() == 0) {
            return Optional.empty();
        }
        String text = nodes.item(0).getTextContent();
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
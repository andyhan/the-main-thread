package com.demo.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TrackImageService {

    public record Point(double lat, double lon) {
    }

    public byte[] renderTrackToPng(List<Point> points) throws Exception {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 points to render a track");
        }

        int width = 900;
        int height = 900;
        int padding = 60;

        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        for (Point p : points) {
            minLat = Math.min(minLat, p.lat());
            maxLat = Math.max(maxLat, p.lat());
            minLon = Math.min(minLon, p.lon());
            maxLon = Math.max(maxLon, p.lon());
        }

        double latSpan = Math.max(1e-12, maxLat - minLat);
        double lonSpan = Math.max(1e-12, maxLon - minLon);

        // Calculate scale to fit track with padding, maintaining aspect ratio
        double scaleX = (width - 2.0 * padding) / lonSpan;
        double scaleY = (height - 2.0 * padding) / latSpan;
        double scale = Math.min(scaleX, scaleY);

        // Calculate actual dimensions of scaled track
        double scaledWidth = lonSpan * scale;
        double scaledHeight = latSpan * scale;

        // Calculate offsets to center the track
        double offsetX = (width - scaledWidth) / 2.0;
        double offsetY = (height - scaledHeight) / 2.0;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Fill background with light gray
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, width, height);

        // Set track color to blue
        g.setColor(new Color(59, 130, 246)); // Blue-500
        g.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Draw track polyline
        for (int i = 1; i < points.size(); i++) {
            var a = points.get(i - 1);
            var b = points.get(i);

            int x1 = toX(a.lon(), minLon, scale, offsetX);
            int y1 = toY(a.lat(), maxLat, scale, offsetY);
            int x2 = toX(b.lon(), minLon, scale, offsetX);
            int y2 = toY(b.lat(), maxLat, scale, offsetY);

            g.drawLine(x1, y1, x2, y2);
        }

        // Start/End markers
        // Start marker in green
        g.setColor(new Color(34, 197, 94)); // Green-500
        drawDot(g, points.getFirst(), minLon, maxLat, scale, offsetX, offsetY, 16);
        // End marker in red
        g.setColor(new Color(239, 68, 68)); // Red-500
        drawDot(g, points.getLast(), minLon, maxLat, scale, offsetX, offsetY, 16);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private int toX(double lon, double minLon, double scale, double offsetX) {
        return (int) Math.round(offsetX + (lon - minLon) * scale);
    }

    private int toY(double lat, double maxLat, double scale, double offsetY) {
        return (int) Math.round(offsetY + (maxLat - lat) * scale);
    }

    private void drawDot(Graphics2D g, Point p, double minLon, double maxLat, double scale, double offsetX, double offsetY, int size) {
        int x = toX(p.lon(), minLon, scale, offsetX);
        int y = toY(p.lat(), maxLat, scale, offsetY);
        int r = size / 2;
        g.fillOval(x - r, y - r, size, size);
    }
}
package com.themainthread.iss.util;

public final class MercatorProjection {

    public static final int MAP_WIDTH = 1280;
    public static final int MAP_HEIGHT = 640;

    private static final double MAX_WEB_MERCATOR_LAT = 85.05112878;

    private MercatorProjection() {
    }

    public static int[] toPixel(double latDeg, double lonDeg) {
        double clampedLat = Math.max(-MAX_WEB_MERCATOR_LAT, Math.min(MAX_WEB_MERCATOR_LAT, latDeg));

        double x = (lonDeg + 180.0) / 360.0 * MAP_WIDTH;

        double latRad = Math.toRadians(clampedLat);
        double mercatorY = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        double y = (MAP_HEIGHT / 2.0) - (MAP_WIDTH / (2.0 * Math.PI)) * mercatorY;

        int pixelX = (int) Math.round(Math.max(0, Math.min(MAP_WIDTH - 1, x)));
        int pixelY = (int) Math.round(Math.max(0, Math.min(MAP_HEIGHT - 1, y)));

        return new int[] { pixelX, pixelY };
    }
}
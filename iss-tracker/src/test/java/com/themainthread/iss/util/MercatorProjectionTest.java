package com.themainthread.iss.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MercatorProjectionTest {

    @Test
    void nullIslandMapsNearCenter() {
        int[] pixels = MercatorProjection.toPixel(0.0, 0.0);

        assertTrue(Math.abs(pixels[0] - 640) <= 2);
        assertTrue(Math.abs(pixels[1] - 320) <= 2);
    }

    @Test
    void easternDatelineMapsNearRightEdge() {
        int[] pixels = MercatorProjection.toPixel(0.0, 179.9);
        assertTrue(pixels[0] > 1260);
    }

    @Test
    void westernDatelineMapsNearLeftEdge() {
        int[] pixels = MercatorProjection.toPixel(0.0, -179.9);
        assertTrue(pixels[0] < 20);
    }
}
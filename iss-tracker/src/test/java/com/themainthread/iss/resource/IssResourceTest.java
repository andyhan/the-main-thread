package com.themainthread.iss.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.themainthread.iss.service.IssPositionCache;
import com.themainthread.iss.service.IssPositionCache.PositionFix;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class IssResourceTest {

    @InjectMock
    IssPositionCache cache;

    @Test
    void returnsCurrentFix() {
        PositionFix fix = new PositionFix(
                51.5074,
                -0.1278,
                640,
                250,
                1716835200L,
                Instant.parse("2026-03-14T10:15:30Z"));

        when(cache.latest()).thenReturn(fix);

        given()
                .when().get("/api/iss/position")
                .then()
                .statusCode(200)
                .body("latitude", is(51.5074f))
                .body("longitude", is(-0.1278f))
                .body("pixelX", is(640))
                .body("pixelY", is(250));
    }

    @Test
    void returns204WhenNoFixExists() {
        when(cache.latest()).thenReturn(null);

        given()
                .when().get("/api/iss/position")
                .then()
                .statusCode(204);
    }
}
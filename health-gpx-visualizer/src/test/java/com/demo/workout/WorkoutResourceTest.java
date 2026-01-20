package com.demo.workout;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class WorkoutResourceTest {

    @Test
    void uploadExtractsHeartRate() {
        File gpx = new File("src/test/resources/sample-with-hr.gpx");

        given()
                .multiPart("file", gpx, "application/gpx+xml")
                .when()
                .post("/workouts")
                .then()
                .statusCode(200)
                .body("name", anyOf(equalTo("HR Test"), equalTo("Untitled Workout")))
                .body("avgHeartRate", is(150))
                .body("maxHeartRate", is(160))
                .body("routeGeoJson", notNullValue());
    }
}
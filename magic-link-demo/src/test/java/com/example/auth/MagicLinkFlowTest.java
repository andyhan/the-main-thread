package com.example.auth;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class MagicLinkFlowTest {

    @Test
    void unknownUserStillReturnsNoContent() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"does-not-exist@example.com\"}")
                .when()
                .post("/auth/magic/request")
                .then()
                .statusCode(204);
    }
}
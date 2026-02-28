package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LoginResourceTest {

    @Test
    void loginReturnsAccessToken() {
        given()
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }

    @Test
    void loginWithBodyReturnsAccessToken() {
        given()
                .contentType("application/json")
                .body("{\"subject\":\"bob\",\"groups\":[\"account-viewer\"]}")
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }
}

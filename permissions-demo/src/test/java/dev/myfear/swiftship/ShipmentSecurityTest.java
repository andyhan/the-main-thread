package dev.myfear.swiftship;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ShipmentSecurityTest {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @Test
    void anonymousUserCannotListShipments() {
        given()
            .when().get("/shipment")
            .then()
            .statusCode(401);
    }

    @Test
    void readerCanGetShipmentById() {
        String token = accessToken("alice", "alice", "shipment:read");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/shipment/1")
            .then()
            .statusCode(200)
            .body("id", equalTo(1))
            .body("trackingNumber", equalTo("SWS-001"))
            .body("destination", equalTo("Berlin"))
            .body("status", equalTo("IN_TRANSIT"));
    }

    @Test
    void readerCannotDeleteShipment() {
        String token = accessToken("alice", "alice", "shipment:read");

        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/shipment/1")
            .then()
            .statusCode(403);
    }

    private String accessToken(String username, String password, String scope) {
        String tokenUrl = authServerUrl + "/protocol/openid-connect/token";
        return given()
            .contentType(ContentType.URLENC)
            .formParam("client_id", "swiftship")
            .formParam("client_secret", "secret")
            .formParam("username", username)
            .formParam("password", password)
            .formParam("grant_type", "password")
            .formParam("scope", scope)
            .when()
            .post(tokenUrl)
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");
    }
}

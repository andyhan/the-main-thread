package com.example.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class KeycloakIntegrationTest {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @Test
    void keycloakRealmIssuesTokenForRealmUser() {
        String tokenUrl = authServerUrl + "/protocol/openid-connect/token";
        given()
                .formParam("grant_type", "password")
                .formParam("client_id", "magic-link-demo")
                .formParam("client_secret", "secret-from-keycloak")
                .formParam("username", "alice")
                .formParam("password", "alice")
                .when()
                .post(tokenUrl)
                .then()
                .statusCode(200)
                .body("access_token", notNullValue())
                .body("token_type", notNullValue());
    }
}

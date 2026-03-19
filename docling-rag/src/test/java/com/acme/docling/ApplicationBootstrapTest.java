package com.acme.docling;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class ApplicationBootstrapTest {

    @Test
    void contextStarts() {
        // Quarkus bootstraps the full application (including Dev Services) before this runs.
    }

    @Test
    void ingestUrlRejectsMissingUrl() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/admin/ingest/url")
                .then()
                .statusCode(400);
    }

    @Test
    void ingestUrlRejectsBlankUrl() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"   \"}")
                .when()
                .post("/admin/ingest/url")
                .then()
                .statusCode(400);
    }

}

package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class DashboardResourceTest {

    @Test
    void dashboardRootReturnsHtmlWithTitleAndStreamClient() {
        given()
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .contentType("text/html;charset=UTF-8")
                .body(containsString("BTC Bollinger Bands"))
                .body(containsString("EventSource(\"/stream\")"));
    }
}

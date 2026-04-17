package com.themainthread.cache;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.themainthread.cache.service.PriceLookupService;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheResourceTest {

    @Inject
    PriceLookupService priceLookup;

    @Test
    @Order(1)
    void firstCallIsACacheMiss() {
        int before = priceLookup.backendCallsFor("SKU-001");

        given()
                .when().get("/prices/SKU-001")
                .then()
                .statusCode(200)
                .body(equalTo("29.99"));

        assertEquals(before + 1, priceLookup.backendCallsFor("SKU-001"),
                "First call should hit the backend");
    }

    @Test
    @Order(2)
    void secondCallIsACacheHit() {
        int before = priceLookup.backendCallsFor("SKU-001");

        given()
                .when().get("/prices/SKU-001")
                .then()
                .statusCode(200)
                .body(equalTo("29.99"));

        given()
                .when().get("/prices/SKU-001")
                .then()
                .statusCode(200)
                .body(equalTo("29.99"));

        assertEquals(before, priceLookup.backendCallsFor("SKU-001"),
                "Cached calls should not hit the backend again");
    }

    @Test
    @Order(3)
    void successfulUpdateInvalidatesCache() {
        int before = priceLookup.backendCallsFor("SKU-002");

        given()
                .when().get("/prices/SKU-002")
                .then()
                .statusCode(200)
                .body(equalTo("49.99"));

        assertEquals(before + 1, priceLookup.backendCallsFor("SKU-002"),
                "Initial GET should populate the cache");

        given()
                .queryParam("price", "59.99")
                .when().put("/prices/SKU-002")
                .then()
                .statusCode(200);

        given()
                .when().get("/prices/SKU-002")
                .then()
                .statusCode(200)
                .body(equalTo("59.99"));

        assertEquals(before + 2, priceLookup.backendCallsFor("SKU-002"),
                "After successful invalidation the next GET should be a cache miss");
    }

    @Test
    @Order(4)
    void failedUpdateDoesNotInvalidateCache() {
        int before = priceLookup.backendCallsFor("SKU-003");

        given()
                .when().get("/prices/SKU-003")
                .then()
                .statusCode(200)
                .body(equalTo("9.99"));

        given()
                .when().get("/prices/SKU-003")
                .then()
                .statusCode(200)
                .body(equalTo("9.99"));

        assertEquals(before + 1, priceLookup.backendCallsFor("SKU-003"),
                "Second GET should be served from cache");

        given()
                .queryParam("price", "12.99")
                .queryParam("fail", true)
                .when().put("/prices/SKU-003")
                .then()
                .statusCode(500);

        given()
                .when().get("/prices/SKU-003")
                .then()
                .statusCode(200)
                .body(equalTo("9.99"));

        assertEquals(before + 1, priceLookup.backendCallsFor("SKU-003"),
                "After failed update the cache should still serve the old value");
    }

    @Test
    @Order(5)
    void forcedInvalidationClearsEvenOnFailure() {
        int before = priceLookup.backendCallsFor("SKU-001");

        given()
                .when().get("/prices/SKU-001")
                .then()
                .statusCode(200)
                .body(equalTo("29.99"));

        int afterWarmup = priceLookup.backendCallsFor("SKU-001");
        assertEquals(before + 1, afterWarmup,
                "Warmup GET should repopulate SKU-001 after earlier invalidation");

        given()
                .when().get("/prices/SKU-001")
                .then()
                .statusCode(200)
                .body(equalTo("29.99"));

        assertEquals(afterWarmup, priceLookup.backendCallsFor("SKU-001"),
                "SKU-001 should be cached before forced invalidation");

        given()
                .queryParam("price", "39.99")
                .queryParam("fail", true)
                .when().put("/prices/SKU-001/force-invalidate")
                .then()
                .statusCode(500);

        given()
                .when().get("/prices/SKU-001")
                .then()
                .statusCode(200)
                .body(equalTo("39.99"));

        assertEquals(afterWarmup + 1, priceLookup.backendCallsFor("SKU-001"),
                "After forced invalidation the next GET should be a cache miss");
    }
}
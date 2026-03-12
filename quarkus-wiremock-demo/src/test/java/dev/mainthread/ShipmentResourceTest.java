package dev.mainthread;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ConnectWireMock
class ShipmentResourceTest {

    WireMock wiremock;

    @BeforeEach
    void resetStubs() {
        wiremock.resetMappings();
    }

    @Test
    void returnsInTransitStatus() {
        wiremock.register(
                get(urlPathEqualTo("/v1/track/SWIFT99887766"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                            {
                                              "tracking_number": "SWIFT99887766",
                                              "status_code": "IT",
                                              "status_message": "Package is in transit",
                                              "estimated_delivery": "2026-03-20T18:00:00Z",
                                              "events": [
                                                {
                                                  "timestamp": "2026-03-18T14:22:00Z",
                                                  "location": "Memphis, TN",
                                                  "event_code": "ARR",
                                                  "description": "Arrived at sorting facility"
                                                }
                                              ]
                                            }
                                        """)));

        given()
                .when().get("/shipments/SWIFT99887766")
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_TRANSIT"))
                .body("lastLocation", equalTo("Memphis, TN"))
                .body("message", equalTo("Package is in transit"));
    }

    @Test
    void shipmentProgressesThroughLifecycle() {
        final String trackingNumber = "SWIFT55443322";
        final String path = "/v1/track/" + trackingNumber;
        final String scenario = "shipment-lifecycle";

        wiremock.register(
                get(urlPathEqualTo(path))
                        .inScenario(scenario)
                        .whenScenarioStateIs("Started")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(carrierResponse(
                                        trackingNumber,
                                        "LC",
                                        "Label created, awaiting pickup",
                                        "unknown")))
                        .willSetStateTo("IN_TRANSIT"));

        wiremock.register(
                get(urlPathEqualTo(path))
                        .inScenario(scenario)
                        .whenScenarioStateIs("IN_TRANSIT")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(carrierResponse(
                                        trackingNumber,
                                        "IT",
                                        "Package is in transit",
                                        "Chicago, IL")))
                        .willSetStateTo("OUT_FOR_DELIVERY"));

        wiremock.register(
                get(urlPathEqualTo(path))
                        .inScenario(scenario)
                        .whenScenarioStateIs("OUT_FOR_DELIVERY")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(carrierResponse(
                                        trackingNumber,
                                        "OD",
                                        "Out for delivery",
                                        "Chicago, IL")))
                        .willSetStateTo("DELIVERED"));

        wiremock.register(
                get(urlPathEqualTo(path))
                        .inScenario(scenario)
                        .whenScenarioStateIs("DELIVERED")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(carrierResponse(
                                        trackingNumber,
                                        "DL",
                                        "Package delivered",
                                        "Chicago, IL"))));

        given()
                .when().get("/shipments/" + trackingNumber)
                .then()
                .statusCode(200)
                .body("status", equalTo("LABEL_CREATED"));

        given()
                .when().get("/shipments/" + trackingNumber)
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_TRANSIT"))
                .body("lastLocation", equalTo("Chicago, IL"));

        given()
                .when().get("/shipments/" + trackingNumber)
                .then()
                .statusCode(200)
                .body("status", equalTo("OUT_FOR_DELIVERY"));

        given()
                .when().get("/shipments/" + trackingNumber)
                .then()
                .statusCode(200)
                .body("status", equalTo("DELIVERED"));
    }

    private String carrierResponse(String trackingNumber, String code, String message, String location) {
        return """
                {
                  "tracking_number": "%s",
                  "status_code": "%s",
                  "status_message": "%s",
                  "estimated_delivery": "2026-03-20T18:00:00Z",
                  "events": [
                    {
                      "timestamp": "2026-03-18T14:22:00Z",
                      "location": "%s",
                      "event_code": "EVT",
                      "description": "%s"
                    }
                  ]
                }
                """.formatted(trackingNumber, code, message, location, message);
    }

    @Test
    void estimatedDeliveryIsPresent() {
        wiremock.register(
                get(urlPathEqualTo("/v1/track/SWIFT11223344"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                        """
                                                    {
                                                      "tracking_number": "SWIFT11223344",
                                                      "status_code": "IT",
                                                      "status_message": "In transit",
                                                      "estimated_delivery": "{{now offset='3 days' format='yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\''}}",
                                                      "events": []
                                                    }
                                                """)));

        given()
                .when().get("/shipments/SWIFT11223344")
                .then()
                .statusCode(200)
                .body("estimatedDelivery", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void returnsExceptionStatusWhenCarrierIsDown() {
        wiremock.register(
                get(urlPathEqualTo("/v1/track/SWIFT00000001"))
                        .willReturn(aResponse()
                                .withStatus(503)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                            {"error":"Service temporarily unavailable"}
                                        """)));

        given()
                .when().get("/shipments/SWIFT00000001")
                .then()
                .statusCode(200)
                .body("status", equalTo("EXCEPTION"))
                .body("message", equalTo("Carrier tracking temporarily unavailable"));
    }

    @Test
    void handlesConnectionReset() {
        wiremock.register(
                get(urlPathEqualTo("/v1/track/SWIFT00000002"))
                        .willReturn(aResponse()
                                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        given()
                .when().get("/shipments/SWIFT00000002")
                .then()
                .statusCode(500);
    }

    @Test
    void slowCarrierResponseCanTriggerTimeouts() {
        wiremock.register(
                get(urlPathEqualTo("/v1/track/SWIFT00000003"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withFixedDelay(2000)
                                .withBody("""
                                            {
                                              "tracking_number": "SWIFT00000003",
                                              "status_code": "IT",
                                              "status_message": "In transit",
                                              "estimated_delivery": "2026-03-20T18:00:00Z",
                                              "events": []
                                            }
                                        """)));

        given()
                .when().get("/shipments/SWIFT00000003")
                .then()
                .statusCode(200);
    }

    @Test
    void callsCarrierApiExactlyTwiceWithoutCaching() {
        wiremock.register(
                get(urlPathEqualTo("/v1/track/SWIFT77665544"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(carrierResponse(
                                        "SWIFT77665544",
                                        "DL",
                                        "Delivered",
                                        "Austin, TX"))));

        given().when().get("/shipments/SWIFT77665544").then().statusCode(200);
        given().when().get("/shipments/SWIFT77665544").then().statusCode(200);

        wiremock.verifyThat(
                exactly(2),
                getRequestedFor(urlPathEqualTo("/v1/track/SWIFT77665544")));
    }

}

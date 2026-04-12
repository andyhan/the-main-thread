package academy.themainthread;

import academy.themainthread.webhook.HmacVerifier;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AcademyWorkflowTest {

    @Test
    void adminCreatesTemplatePartnerWebhookIssuesAssertionAndPublicJsonMatches() throws Exception {
        String templateId = given().contentType(ContentType.JSON)
                .body(
                        """
                        {
                          "name": "Quarkus Developer",
                          "description": "Demonstrates proficiency with Quarkus.",
                          "criteria": "Complete the fundamentals course.",
                          "imageUrl": "https://design.jboss.org/quarkus/logo/final/SVG/quarkus_icon_rgb_default.svg",
                          "skills": "Quarkus,Java"
                        }
                        """)
                .when()
                .post("/admin/badges")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String partnerId = given().contentType(ContentType.JSON)
                .body(
                        """
                        {
                          "name": "Acme Training",
                          "webhookSecret": "super-secret-signing-key-change-in-production"
                        }
                        """)
                .when()
                .post("/admin/partners")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given().contentType(ContentType.JSON)
                .body(
                        """
                        {
                          "templateId": "%s",
                          "courseId": "QUARKUS-FUND-101"
                        }
                        """
                                .formatted(templateId))
                .when()
                .post("/admin/partners/" + partnerId + "/courses")
                .then()
                .statusCode(200);

        String payload =
                """
                {
                  "partnerId": "%s",
                  "courseId": "QUARKUS-FUND-101",
                  "learnerEmail": "alice@example.com",
                  "learnerName": "Alice Smith",
                  "completedAt": "2026-04-06T14:00:00Z",
                  "idempotencyKey": "evt-%s"
                }
                """
                        .formatted(partnerId, UUID.randomUUID());

        String sig = "sha256=" + HmacVerifier.compute(payload, "super-secret-signing-key-change-in-production");

        given().contentType(ContentType.JSON)
                .header("X-Partner-Id", partnerId)
                .header("X-Webhook-Signature", sig)
                .body(payload)
                .when()
                .post("/webhooks/completions")
                .then()
                .statusCode(202)
                .body("status", equalTo("accepted"))
                .body("eventId", notNullValue());

        String assertionId = waitForAssertionId();

        given().header("Accept", "application/json")
                .when()
                .get("/assertions/" + assertionId)
                .then()
                .statusCode(200)
                .body("type", equalTo("Assertion"))
                .body("recipient.hashed", equalTo(true))
                .body("recipient.identity", containsString("sha256$"));

        given().when().get("/keys/1").then().statusCode(200).body("publicKeyPem", containsString("BEGIN PUBLIC KEY"));

        String earnerId = given().when().get("/admin/assertions").then().statusCode(200).extract().path("[0].earner.id");

        given().when()
                .get("/earners/" + earnerId)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @SuppressWarnings("unchecked")
    private static String waitForAssertionId() throws InterruptedException {
        for (int i = 0; i < 80; i++) {
            List<Map<String, Object>> list = given().when()
                    .get("/admin/assertions")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(List.class);
            if (list != null && !list.isEmpty()) {
                Object id = list.get(0).get("id");
                assertNotNull(id);
                return id.toString();
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for issued assertion");
    }
}

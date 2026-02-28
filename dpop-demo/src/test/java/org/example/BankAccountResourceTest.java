package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.security.KeyPairGenerator;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BankAccountResourceTest {

    private static final String INVALID_DPOP_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6ImRwb3AranV0In0.e30.fake";

    @Test
    void invalidProofReturns401WithChallenge() {
        given()
                .header("Authorization", "DPoP " + INVALID_DPOP_JWT)
                .header("DPoP", INVALID_DPOP_JWT)
                .when()
                .get("/accounts/ACC-001/balance")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", notNullValue());
    }

    @Test
    void fullFlowLoginThenBalanceWithDpopProofReturns200() throws Exception {
        // 1. Login to get an access token
        String accessToken = given()
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");

        // 2. Client key pair for DPoP proof (simulates the client's binding key)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair clientKeyPair = kpg.generateKeyPair();

        String balancePath = "/accounts/ACC-001/balance";
        String baseUri = RestAssured.baseURI + ":" + RestAssured.port;
        String balanceUrl = baseUri + balancePath;

        // 3. First request: token + proof with wrong nonce → 401, server sends DPoP-Nonce
        String proofWithWrongNonce = DPoPProofBuilder.build(clientKeyPair, accessToken, "wrong-nonce", "GET", balanceUrl);
        Response challengeResponse = given()
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", proofWithWrongNonce)
                .when()
                .get(balancePath);
        challengeResponse.then().statusCode(401);
        String nonce = challengeResponse.header("DPoP-Nonce");
        if (nonce == null || nonce.isBlank()) {
            nonce = extractNonceFromWwwAuth(challengeResponse.header("WWW-Authenticate"));
        }
        if (nonce == null || nonce.isBlank()) {
            throw new AssertionError("401 response must include DPoP-Nonce; got WWW-Authenticate: " + challengeResponse.header("WWW-Authenticate"));
        }

        // 4. Build a valid DPoP proof (binds token + nonce + method + URL to client key)
        String proof = DPoPProofBuilder.build(clientKeyPair, accessToken, nonce, "GET", balanceUrl);

        // 5. Second request: token + valid proof → 200 and balance response
        given()
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", proof)
                .when()
                .get(balancePath)
                .then()
                .statusCode(200)
                .body("accountId", equalTo("ACC-001"))
                .body("owner", notNullValue())
                .body("balancePence", equalTo(1_234_567));
    }

    private static String extractNonceFromWwwAuth(String wwwAuth) {
        if (wwwAuth == null) return null;
        // DPoP-Nonce may be sent as a separate header; if only in WWW-Authenticate, parse it
        int i = wwwAuth.indexOf("nonce=\"");
        if (i < 0) return null;
        int start = i + 7;
        int end = wwwAuth.indexOf('"', start);
        return end > start ? wwwAuth.substring(start, end) : null;
    }
}
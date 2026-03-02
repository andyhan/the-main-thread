package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.security.KeyPairGenerator;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class BankAccountResourceTest {

    private static final String INVALID_DPOP_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6ImRwb3AranV0In0.e30.fake";

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

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
    void fullFlowKeycloakTokenThenBalanceWithDpopProofReturns200() throws Exception {
        String tokenEndpointUrl = authServerUrl + "/protocol/openid-connect/token";

        // 1. Client key pair (same key used for Keycloak and for Quarkus)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair clientKeyPair = kpg.generateKeyPair();

        // 2. DPoP proof for Keycloak token request (no ath, no nonce)
        String proofForKeycloak = DPoPProofBuilder.buildForKeycloak(clientKeyPair, "POST", tokenEndpointUrl);

        // 3. Request token from Keycloak with DPoP; token will contain cnf (thumbprint of our public key)
        String accessToken = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("username", "alice")
                .formParam("password", "alice")
                .formParam("client_id", "dpop-api")
                .header("DPoP", proofForKeycloak)
                .when()
                .post(tokenEndpointUrl)
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");

        String balancePath = "/accounts/ACC-001/balance";
        String baseUri = RestAssured.baseURI + ":" + RestAssured.port;
        String balanceUrl = baseUri + balancePath;

        // 4. First request: token + proof without nonce (or wrong nonce) → 401, server sends DPoP-Nonce
        String proofNoNonce = DPoPProofBuilder.build(clientKeyPair, accessToken, null, "GET", balanceUrl);
        Response challengeResponse = given()
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", proofNoNonce)
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

        // 5. Build proof with server-issued nonce
        String proof = DPoPProofBuilder.build(clientKeyPair, accessToken, nonce, "GET", balanceUrl);

        // 6. Second request: token + valid proof → 200
        given()
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", proof)
                .when()
                .get(balancePath)
                .then()
                .statusCode(200)
                .body("accountId", equalTo("ACC-001"))
                .body("owner", equalTo("alice"))
                .body("balancePence", equalTo(1_234_567));
    }

    @Test
    void tokenWithWrongKeyReturns401() throws Exception {
        String tokenEndpointUrl = authServerUrl + "/protocol/openid-connect/token";

        // Key pair A: used when obtaining the token (Keycloak binds token to this key)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair keyPairA = kpg.generateKeyPair();
        String proofForKeycloak = DPoPProofBuilder.buildForKeycloak(keyPairA, "POST", tokenEndpointUrl);

        String accessToken = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("username", "alice")
                .formParam("password", "alice")
                .formParam("client_id", "dpop-api")
                .header("DPoP", proofForKeycloak)
                .when()
                .post(tokenEndpointUrl)
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");

        // Key pair B: different key — proof will not match token's cnf
        java.security.KeyPair keyPairB = kpg.generateKeyPair();
        String balancePath = "/accounts/ACC-001/balance";
        String baseUri = RestAssured.baseURI + ":" + RestAssured.port;
        String balanceUrl = baseUri + balancePath;
        // First get a nonce (using correct key)
        String proofA = DPoPProofBuilder.build(keyPairA, accessToken, null, "GET", balanceUrl);
        Response challengeResp = given()
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", proofA)
                .when()
                .get(balancePath);
        String nonce = challengeResp.header("DPoP-Nonce");
        if (nonce == null || nonce.isBlank()) {
            nonce = extractNonceFromWwwAuth(challengeResp.header("WWW-Authenticate"));
        }

        // Call with proof signed by key pair B (wrong key) → 401
        String proofB = DPoPProofBuilder.build(keyPairB, accessToken, nonce != null ? nonce : "dummy", "GET", balanceUrl);
        given()
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", proofB)
                .when()
                .get(balancePath)
                .then()
                .statusCode(401);
    }

    private static String extractNonceFromWwwAuth(String wwwAuth) {
        if (wwwAuth == null) return null;
        int i = wwwAuth.indexOf("nonce=\"");
        if (i < 0) return null;
        int start = i + 7;
        int end = wwwAuth.indexOf('"', start);
        return end > start ? wwwAuth.substring(start, end) : null;
    }
}

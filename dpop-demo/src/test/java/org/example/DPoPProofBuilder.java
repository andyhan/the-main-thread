package org.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Builds a DPoP proof JWT (RFC 9449) for use in tests.
 * The proof binds the request (method, URL) and access token (ath) to the client's key.
 */
public final class DPoPProofBuilder {

    private DPoPProofBuilder() {
    }

    /**
     * Builds a signed DPoP proof JWT.
     *
     * @param clientKeyPair  client's RSA key pair (proof is signed with the private key)
     * @param accessToken    the access token (used to compute ath)
     * @param nonce          server-issued nonce from a 401 response
     * @param method         HTTP method (e.g. GET)
     * @param url            full request URL (e.g. http://localhost:8081/accounts/ACC-001/balance)
     * @return signed DPoP proof JWT string
     */
    public static String build(
            java.security.KeyPair clientKeyPair,
            String accessToken,
            String nonce,
            String method,
            String url) throws Exception {
        RSAPrivateKey privateKey = (RSAPrivateKey) clientKeyPair.getPrivate();
        JWK jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) clientKeyPair.getPublic()).build();

        // ath = base64url(SHA-256(access_token))
        byte[] tokenBytes = accessToken.getBytes(StandardCharsets.US_ASCII);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(tokenBytes);
        String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("dpop+jwt"))
                .jwk(jwk)
                .build();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("iat", Instant.now().getEpochSecond())
                .claim("jti", UUID.randomUUID().toString())
                .claim("nonce", nonce)
                .claim("ath", ath)
                .claim("htm", method)
                .claim("htu", url)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        JWSSigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}

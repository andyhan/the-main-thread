package academy.themainthread.badge;

import academy.themainthread.domain.BadgeAssertion;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AssertionSigner {

    @ConfigProperty(name = "academy.base-url")
    String baseUrl;

    public String sign(BadgeAssertion assertion) {
        String assertionUrl = baseUrl + "/assertions/" + assertion.id;
        String badgeUrl = baseUrl + "/badges/" + assertion.template.id;
        String identity = RecipientIdentity.openBadgeIdentity(assertion.earner.email, assertion.salt);

        Map<String, Object> recipientClaim = Map.of(
                "type", "email",
                "hashed", true,
                "salt", assertion.salt,
                "identity", identity);

        Map<String, Object> verificationClaim = Map.of(
                "type", "signed",
                "creator", baseUrl + "/keys/1");

        long expEpochSeconds = assertion.expiresAt != null
                ? assertion.expiresAt.getEpochSecond()
                : Instant.now().plus(3650, ChronoUnit.DAYS).getEpochSecond();

        return Jwt.claims()
                .claim("@context", "https://w3id.org/openbadges/v2")
                .claim("type", "Assertion")
                .claim("id", assertionUrl)
                .claim("recipient", recipientClaim)
                .claim("badge", badgeUrl)
                .claim("issuedOn", assertion.issuedOn.toString())
                .claim("verification", verificationClaim)
                .expiresAt(expEpochSeconds)
                .jws()
                .sign();
    }

    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
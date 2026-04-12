package academy.themainthread.badge;

import academy.themainthread.domain.BadgeAssertion;
import academy.themainthread.domain.BadgeTemplate;
import academy.themainthread.domain.Earner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@ApplicationScoped
public class BadgeIssuanceService {

    @Inject
    AssertionSigner signer;

    @Inject
    Event<BadgeIssuedEvent> badgeIssuedEvent;

    @Transactional
    public BadgeAssertion issue(Earner earner, BadgeTemplate template, Instant expiresAt) {
        BadgeAssertion assertion = new BadgeAssertion();
        assertion.id = UUID.randomUUID();
        assertion.earner = earner;
        assertion.template = template;
        assertion.issuedOn = Instant.now();
        assertion.expiresAt = expiresAt;
        assertion.salt = AssertionSigner.generateSalt();
        assertion.signedToken = signer.sign(assertion);
        assertion.persist();

        badgeIssuedEvent.fire(new BadgeIssuedEvent(assertion.id, earner.email, earner.name, template.name));

        return assertion;
    }

    @Transactional
    public BadgeAssertion issueWithDefaultExpiry(Earner earner, BadgeTemplate template) {
        Instant expires = Instant.now().plus(365L * 2L, ChronoUnit.DAYS);
        return issue(earner, template, expires);
    }

    @Transactional
    public void revoke(UUID assertionId, String reason) {
        BadgeAssertion assertion = BadgeAssertion.findById(assertionId);
        if (assertion == null) {
            throw new IllegalArgumentException("Assertion not found: " + assertionId);
        }
        assertion.revoked = true;
        assertion.revokeReason = reason;
        assertion.persist();
    }
}
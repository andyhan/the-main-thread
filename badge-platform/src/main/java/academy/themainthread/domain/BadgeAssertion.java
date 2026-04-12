package academy.themainthread.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "badge_assertion")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BadgeAssertion extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "earner_id", nullable = false)
    public Earner earner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    public BadgeTemplate template;

    @Column(name = "issued_on", nullable = false)
    public Instant issuedOn = Instant.now();

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean revoked = false;

    @Column(name = "revoke_reason")
    public String revokeReason;

    @Column(name = "signed_token", nullable = false, columnDefinition = "TEXT")
    public String signedToken;

    @Column(nullable = false, length = 64)
    public String salt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public static BadgeAssertion findByIdWithDetails(UUID id) {
        return find(
                "SELECT a FROM BadgeAssertion a JOIN FETCH a.earner JOIN FETCH a.template WHERE a.id = ?1",
                id)
                .firstResult();
    }
}
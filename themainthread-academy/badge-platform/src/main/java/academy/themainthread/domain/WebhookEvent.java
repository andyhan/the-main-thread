package academy.themainthread.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_event")
public class WebhookEvent extends PanacheEntityBase {

    public enum Status {
        RECEIVED,
        PROCESSED,
        FAILED,
        DUPLICATE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    public AccreditedPartner partner;

    @Column(name = "idempotency_key", nullable = false)
    public String idempotencyKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Status status = Status.RECEIVED;

    @Column(name = "received_at", nullable = false)
    public Instant receivedAt = Instant.now();

    @Column(name = "processed_at")
    public Instant processedAt;

    @Column(columnDefinition = "TEXT")
    public String error;

    public static boolean isDuplicate(AccreditedPartner partner, String idempotencyKey) {
        return count("partner = ?1 AND idempotencyKey = ?2", partner, idempotencyKey) > 0;
    }
}
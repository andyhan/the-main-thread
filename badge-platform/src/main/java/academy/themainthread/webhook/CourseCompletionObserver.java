package academy.themainthread.webhook;

import academy.themainthread.badge.BadgeIssuanceService;
import academy.themainthread.domain.AccreditedPartner;
import academy.themainthread.domain.Earner;
import academy.themainthread.domain.PartnerBadgeTemplate;
import academy.themainthread.domain.WebhookEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class CourseCompletionObserver {

    private static final Logger LOG = Logger.getLogger(CourseCompletionObserver.class);

    @Inject
    BadgeIssuanceService issuanceService;

    @Transactional(TxType.REQUIRES_NEW)
    public void onCompletion(@Observes CourseCompletionEvent event) {
        UUID eventId = event.webhookEventId();
        WebhookEvent webhookEvent = WebhookEvent.findById(eventId);
        if (webhookEvent == null) {
            LOG.errorf("WebhookEvent missing for async completion: %s", eventId);
            return;
        }

        try {
            Earner earner = Earner.findByEmail(event.learnerEmail());
            if (earner == null) {
                earner = new Earner();
                earner.email = event.learnerEmail();
                earner.name = event.learnerName();
                earner.persist();
            }

            AccreditedPartner partner = event.partner();
            PartnerBadgeTemplate mapping = PartnerBadgeTemplate.findByCourseId(partner, event.courseId());

            if (mapping == null) {
                LOG.warnf("No badge template mapped for partner %s, course %s", partner.id, event.courseId());
                webhookEvent.status = WebhookEvent.Status.FAILED;
                webhookEvent.error = "No badge template mapped for course: " + event.courseId();
                webhookEvent.processedAt = Instant.now();
                webhookEvent.persist();
                return;
            }

            issuanceService.issueWithDefaultExpiry(earner, mapping.template);

            webhookEvent.status = WebhookEvent.Status.PROCESSED;
            webhookEvent.processedAt = Instant.now();
            webhookEvent.persist();

            LOG.infof(
                    "Badge '%s' issued to %s via webhook from partner %s",
                    mapping.template.name, event.learnerEmail(), partner.name);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process course completion for event %s", eventId);
            webhookEvent.status = WebhookEvent.Status.FAILED;
            webhookEvent.error = e.getMessage();
            webhookEvent.processedAt = Instant.now();
            webhookEvent.persist();
        }
    }
}
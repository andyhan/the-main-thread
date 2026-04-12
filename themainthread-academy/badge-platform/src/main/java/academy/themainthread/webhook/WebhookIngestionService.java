package academy.themainthread.webhook;

import academy.themainthread.domain.AccreditedPartner;
import academy.themainthread.domain.WebhookEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class WebhookIngestionService {

    @Transactional
    public WebhookEvent recordReceived(AccreditedPartner partner, String idempotencyKey, String rawBody) {
        WebhookEvent event = new WebhookEvent();
        event.partner = partner;
        event.idempotencyKey = idempotencyKey;
        event.payload = rawBody;
        event.status = WebhookEvent.Status.RECEIVED;
        event.persist();
        return event;
    }
}
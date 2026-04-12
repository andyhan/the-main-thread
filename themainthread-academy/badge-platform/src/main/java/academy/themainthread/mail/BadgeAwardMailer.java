package academy.themainthread.mail;

import academy.themainthread.badge.BadgeIssuedEvent;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BadgeAwardMailer {

    private static final Logger LOG = Logger.getLogger(BadgeAwardMailer.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "academy.base-url")
    String baseUrl;

    public void onBadgeIssued(@ObservesAsync BadgeIssuedEvent event) {
        String assertionUrl = baseUrl + "/assertions/" + event.assertionId();

        String html = """
            <div style="font-family:Georgia,serif;max-width:600px;margin:0 auto;padding:2rem;">
              <h1 style="color:#1a1a2e;">Congratulations, %s!</h1>
              <p style="margin:1rem 0;line-height:1.6;">
                You have earned the <strong>%s</strong> credential from
                TheMainThread Academy.
              </p>
              <p style="margin:1rem 0;line-height:1.6;">
                Your badge is cryptographically signed and publicly verifiable.
                Share it with confidence.
              </p>
              <a href="%s"
                 style="display:inline-block;background:#e94560;color:white;
                        text-decoration:none;padding:0.75rem 1.5rem;
                        border-radius:6px;font-weight:600;margin-top:1rem;">
                View your credential →
              </a>
              <p style="margin-top:2rem;font-size:0.8rem;color:#6b7280;">
                This credential complies with the Open Badge 2.0 specification
                and can be verified at the link above.
              </p>
            </div>
            """
                .formatted(event.earnerName(), event.badgeName(), assertionUrl);

        try {
            mailer.send(Mail.withHtml(
                    event.earnerEmail(),
                    "You earned: " + event.badgeName() + " — TheMainThread Academy",
                    html));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send badge award email to %s", event.earnerEmail());
        }
    }
}
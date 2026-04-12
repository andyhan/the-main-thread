package academy.themainthread.webhook;

import academy.themainthread.domain.AccreditedPartner;
import academy.themainthread.domain.WebhookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/webhooks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class WebhookResource {

    private static final Logger LOG = Logger.getLogger(WebhookResource.class);

    @Inject
    Event<CourseCompletionEvent> completionEvent;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Validator validator;

    @Inject
    WebhookIngestionService ingestionService;

    @POST
    @Path("/completions")
    public Response receiveCompletion(
            @HeaderParam("X-Webhook-Signature") String signature,
            @HeaderParam("X-Partner-Id") String partnerIdHeader,
            String rawBody) {
        if (partnerIdHeader == null || partnerIdHeader.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Missing X-Partner-Id header"))
                    .build();
        }

        AccreditedPartner partner;
        try {
            partner = AccreditedPartner.findById(UUID.fromString(partnerIdHeader));
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid partner ID"))
                    .build();
        }

        if (partner == null || !partner.active) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Unknown or inactive partner"))
                    .build();
        }

        if (!HmacVerifier.verify(rawBody, signature, partner.webhookSecret)) {
            LOG.warnf("Webhook signature verification failed for partner %s", partner.id);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Invalid signature"))
                    .build();
        }

        CourseCompletionPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, CourseCompletionPayload.class);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid payload: " + e.getMessage()))
                    .build();
        }

        Set<ConstraintViolation<CourseCompletionPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(msg))
                    .build();
        }

        if (!payload.partnerId().equals(partner.id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("partnerId in body must match X-Partner-Id"))
                    .build();
        }

        if (WebhookEvent.isDuplicate(partner, payload.idempotencyKey())) {
            LOG.infof("Duplicate webhook event ignored: %s", payload.idempotencyKey());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Duplicate event — already processed"))
                    .build();
        }

        WebhookEvent event = ingestionService.recordReceived(partner, payload.idempotencyKey(), rawBody);

        completionEvent.fire(new CourseCompletionEvent(
                event.id, partner, payload.courseId(), payload.learnerEmail(), payload.learnerName()));

        return Response.status(Response.Status.ACCEPTED)
                .entity(new AckResponse("accepted", event.id))
                .build();
    }

    public record ErrorResponse(String message) {}

    public record AckResponse(String status, UUID eventId) {}
}
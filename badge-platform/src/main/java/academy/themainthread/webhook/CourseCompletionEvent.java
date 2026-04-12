package academy.themainthread.webhook;

import academy.themainthread.domain.AccreditedPartner;

import java.util.UUID;

public record CourseCompletionEvent(
        UUID webhookEventId,
        AccreditedPartner partner,
        String courseId,
        String learnerEmail,
        String learnerName) {}
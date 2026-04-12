package academy.themainthread.webhook;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CourseCompletionPayload(
        @NotNull UUID partnerId,
        @NotBlank String courseId,
        @NotBlank @Email String learnerEmail,
        @NotBlank String learnerName,
        Instant completedAt,
        @NotBlank String idempotencyKey) {}
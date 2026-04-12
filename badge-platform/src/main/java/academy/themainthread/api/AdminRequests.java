package academy.themainthread.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class AdminRequests {

    private AdminRequests() {}

    public record CreateBadgeTemplate(
            @NotBlank String name,
            @NotBlank String description,
            @NotBlank String criteria,
            @NotBlank String imageUrl,
            String skills) {}

    public record CreateEarner(@NotBlank @Email String email, @NotBlank String name) {}

    public record IssueBadge(@NotNull UUID earnerId, @NotNull UUID templateId) {}

    public record RegisterPartner(@NotBlank String name, @NotBlank String webhookSecret) {}

    public record MapPartnerCourse(@NotNull UUID templateId, @NotBlank String courseId) {}
}
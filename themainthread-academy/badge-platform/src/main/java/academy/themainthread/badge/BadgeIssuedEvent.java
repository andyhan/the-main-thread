package academy.themainthread.badge;

import java.util.UUID;

public record BadgeIssuedEvent(UUID assertionId, String earnerEmail, String earnerName, String badgeName) {}
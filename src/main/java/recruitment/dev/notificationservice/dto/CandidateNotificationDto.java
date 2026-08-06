package recruitment.dev.notificationservice.dto;

import java.time.LocalDateTime;

/** Notification history exposed to the authenticated candidate. Contact details are deliberately omitted. */
public record CandidateNotificationDto(
        Long id,
        Long applicationId,
        NotificationType type,
        String subject,
        String message,
        String status,
        String channel,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

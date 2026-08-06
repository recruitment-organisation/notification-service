package recruitment.dev.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record SendNotificationRequest(
        Long candidateId,
        String candidateKeycloakId,
        Long applicationId,
        @Email String recipientEmail,
        @NotNull NotificationType type,
        String subject,
        String message
) {
}

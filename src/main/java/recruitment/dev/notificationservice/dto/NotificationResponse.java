package recruitment.dev.notificationservice.dto;

public record NotificationResponse(
        String status,
        String channel,
        String recipientEmail,
        String subject
) {
}

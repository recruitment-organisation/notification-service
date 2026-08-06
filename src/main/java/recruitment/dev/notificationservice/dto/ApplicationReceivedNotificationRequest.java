package recruitment.dev.notificationservice.dto;

import jakarta.validation.constraints.NotNull;

public record ApplicationReceivedNotificationRequest(@NotNull Long applicationId) {
}

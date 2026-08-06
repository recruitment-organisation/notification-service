package recruitment.dev.notificationservice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import recruitment.dev.notificationservice.dto.NotificationType;
import recruitment.dev.notificationservice.persistence.NotificationTypeConverter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long candidateId;
    private String candidateKeycloakId;
    private Long applicationId;
    private String recipientEmail;
    private String audienceRole;

    @Column(unique = true)
    private String deliveryKey;

    @Convert(converter = NotificationTypeConverter.class)
    private NotificationType type;

    private String subject;
    private String message;
    private String status;
    private String channel;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

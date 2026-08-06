package recruitment.dev.notificationservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_reads", uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "reader_keycloak_id"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "reader_keycloak_id", nullable = false)
    private String readerKeycloakId;

    @Column(nullable = false)
    private LocalDateTime readAt;

    public NotificationRead(Notification notification, String readerKeycloakId, LocalDateTime readAt) {
        this.notification = notification;
        this.readerKeycloakId = readerKeycloakId;
        this.readAt = readAt;
    }
}

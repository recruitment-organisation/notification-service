package recruitment.dev.notificationservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import recruitment.dev.notificationservice.entities.NotificationRead;

import java.util.Optional;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    Optional<NotificationRead> findByNotificationIdAndReaderKeycloakId(Long notificationId, String readerKeycloakId);
}

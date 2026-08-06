package recruitment.dev.notificationservice.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import recruitment.dev.notificationservice.entities.Notification;

import java.util.Collection;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByCandidateKeycloakIdOrderByCreatedAtDesc(String candidateKeycloakId, Pageable pageable);
    Page<Notification> findByCandidateKeycloakIdIsNullOrderByCreatedAtDesc(Pageable pageable);
    Optional<Notification> findByDeliveryKey(String deliveryKey);

    @Query("""
            select notification from Notification notification
            where notification.candidateKeycloakId = :recipientKeycloakId
               or notification.audienceRole in :roles
            order by notification.createdAt desc
            """)
    Page<Notification> findVisibleForRecipient(
            @Param("recipientKeycloakId") String recipientKeycloakId,
            @Param("roles") Collection<String> roles,
            Pageable pageable
    );
}

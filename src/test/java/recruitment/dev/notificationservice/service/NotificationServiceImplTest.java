package recruitment.dev.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import recruitment.dev.notificationservice.dto.CandidateNotificationDto;
import recruitment.dev.notificationservice.dto.NotificationResponse;
import recruitment.dev.notificationservice.dto.NotificationType;
import recruitment.dev.notificationservice.dto.SendNotificationRequest;
import recruitment.dev.notificationservice.entities.Notification;
import recruitment.dev.notificationservice.entities.NotificationRead;
import recruitment.dev.notificationservice.repositories.NotificationReadRepository;
import recruitment.dev.notificationservice.repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationReadRepository notificationReadRepository = mock(NotificationReadRepository.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl(notificationRepository, notificationReadRepository);

    @Test
    void storesAnInAppNotificationWithoutSendingEmail() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = service.send(new SendNotificationRequest(
                7L, "candidate-keycloak-id", 12L, "candidate@test.local", NotificationType.WELCOME, null, null));

        assertThat(response.status()).isEqualTo("DELIVERED");
        assertThat(response.channel()).isEqualTo("IN_APP");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void returnsRoleAudienceNotificationsForTheAuthenticatedUser() {
        PageRequest pageable = PageRequest.of(0, 20);
        Notification notification = notification(23L, null, "HR");
        when(notificationRepository.findVisibleForRecipient("hr-keycloak-id", Set.of("HR"), pageable))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));
        when(notificationReadRepository.findByNotificationIdAndReaderKeycloakId(23L, "hr-keycloak-id"))
                .thenReturn(Optional.empty());

        Page<CandidateNotificationDto> result = service.findMine("hr-keycloak-id", Set.of("HR"), pageable);

        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(23L);
            assertThat(item.readAt()).isNull();
        });
    }

    @Test
    void marksAnAudienceNotificationAsReadForOneUserOnly() {
        Notification notification = notification(24L, null, "MANAGER");
        when(notificationRepository.findById(24L)).thenReturn(Optional.of(notification));
        when(notificationReadRepository.findByNotificationIdAndReaderKeycloakId(24L, "manager-keycloak-id"))
                .thenReturn(Optional.empty());
        when(notificationReadRepository.save(any(NotificationRead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateNotificationDto result = service.markAsRead(24L, "manager-keycloak-id", Set.of("MANAGER"));

        assertThat(result.readAt()).isNotNull();
        verify(notificationReadRepository).save(any(NotificationRead.class));
    }

    @Test
    void ignoresDuplicateKafkaDeliveries() throws Exception {
        when(notificationRepository.findByDeliveryKey(any())).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ObjectMapper objectMapper = new ObjectMapper();
        service.consumeEvent(objectMapper.readTree("""
                {"eventId":"event-1","eventType":"candidate.created","firstName":"Ada","lastName":"Lovelace"}
                """));

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository).findByDeliveryKey(eq("event-1:role:HR"));
        verify(notificationRepository).findByDeliveryKey(eq("event-1:role:MANAGER"));
    }

    @Test
    void refusesReadingAHiddenNotification() {
        when(notificationRepository.findById(25L)).thenReturn(Optional.of(notification(25L, "another-candidate", null)));

        assertThatThrownBy(() -> service.markAsRead(25L, "candidate-keycloak-id", Set.of("CANDIDATE")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private Notification notification(Long id, String candidateKeycloakId, String audienceRole) {
        return Notification.builder()
                .id(id)
                .candidateId(7L)
                .candidateKeycloakId(candidateKeycloakId)
                .audienceRole(audienceRole)
                .applicationId(12L)
                .type(NotificationType.APPLICATION_RECEIVED)
                .subject("Nouvelle candidature reçue")
                .message("Une candidature a été reçue.")
                .status("DELIVERED")
                .channel("IN_APP")
                .createdAt(LocalDateTime.parse("2026-01-01T10:00:00"))
                .build();
    }
}

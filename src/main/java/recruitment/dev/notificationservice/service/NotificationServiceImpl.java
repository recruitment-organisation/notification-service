package recruitment.dev.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recruitment.dev.notificationservice.dto.CandidateNotificationDto;
import recruitment.dev.notificationservice.dto.NotificationResponse;
import recruitment.dev.notificationservice.dto.NotificationType;
import recruitment.dev.notificationservice.dto.SendNotificationRequest;
import recruitment.dev.notificationservice.entities.Notification;
import recruitment.dev.notificationservice.entities.NotificationRead;
import recruitment.dev.notificationservice.exception.NotificationNotFoundException;
import recruitment.dev.notificationservice.repositories.NotificationReadRepository;
import recruitment.dev.notificationservice.repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final OfferEmailService offerEmailService;

    @Override
    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        String subject = resolveSubject(request);
        String message = resolveMessage(request);
        EmailDeliveryResult emailDelivery = request.type() == NotificationType.OFFER_ACCEPTED
                ? offerEmailService.sendOffer(request.recipientEmail(), subject, message)
                : EmailDeliveryResult.DISABLED;
        String status = emailDelivery == EmailDeliveryResult.FAILED ? "EMAIL_FAILED" : "DELIVERED";
        String channel = emailDelivery == EmailDeliveryResult.SENT ? "IN_APP_EMAIL" : "IN_APP";
        Notification notification = notificationRepository.save(Notification.builder()
                .candidateId(request.candidateId())
                .candidateKeycloakId(request.candidateKeycloakId())
                .applicationId(request.applicationId())
                .recipientEmail(request.recipientEmail())
                .type(request.type())
                .subject(subject)
                .message(message)
                .status(status)
                .channel(channel)
                .createdAt(LocalDateTime.now())
                .build());
        return new NotificationResponse(notification.getStatus(), notification.getChannel(), request.recipientEmail(), subject);
    }

    @Override
    @Transactional
    public void consumeEvent(JsonNode event) {
        String eventId = text(event, "eventId");
        String eventType = text(event, "eventType");
        if (eventId == null || eventType == null) {
            log.warn("Ignoring event without eventId or eventType");
            return;
        }

        switch (eventType) {
            case "candidate.created" -> createCandidateNotifications(eventId, event);
            case "application.submitted" -> createApplicationSubmittedNotifications(eventId, event);
            case "application.updated" -> createApplicationUpdatedNotifications(eventId, event);
            case "job-offer.created" -> createJobOfferNotifications(eventId, event, false);
            case "job-offer.updated" -> createJobOfferNotifications(eventId, event, true);
            default -> log.debug("No notification policy for event type {}", eventType);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateNotificationDto> findMine(String keycloakId, Set<String> roles, Pageable pageable) {
        return notificationRepository.findVisibleForRecipient(keycloakId, roles, pageable)
                .map(notification -> toDto(notification, keycloakId));
    }

    @Override
    @Transactional
    public CandidateNotificationDto markAsRead(Long notificationId, String keycloakId, Set<String> roles) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!isVisibleTo(notification, keycloakId, roles)) {
            throw new AccessDeniedException("This notification is not addressed to the authenticated user");
        }
        NotificationRead read = notificationReadRepository
                .findByNotificationIdAndReaderKeycloakId(notificationId, keycloakId)
                .orElseGet(() -> notificationReadRepository.save(new NotificationRead(notification, keycloakId, LocalDateTime.now())));
        return toDto(notification, read.getReadAt());
    }

    private void createCandidateNotifications(String eventId, JsonNode event) {
        String label = Stream.of(text(event, "firstName"), text(event, "lastName"))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        String candidate = label.isBlank() ? "Un candidat" : label;
        createRoleNotification(eventId, "HR", NotificationType.CANDIDATE_REGISTERED,
                "Nouveau candidat inscrit", candidate + " vient de créer son profil candidat.", null, null);
        createRoleNotification(eventId, "MANAGER", NotificationType.CANDIDATE_REGISTERED,
                "Nouveau candidat inscrit", candidate + " vient de créer son profil candidat.", null, null);
    }

    private void createApplicationSubmittedNotifications(String eventId, JsonNode event) {
        Long applicationId = longValue(event, "applicationId");
        Long candidateId = longValue(event, "candidateId");
        Long jobOfferId = longValue(event, "jobOfferId");
        String message = "Une nouvelle candidature #" + applicationId + " a été déposée pour l’offre #" + jobOfferId + ".";
        createRoleNotification(eventId, "HR", NotificationType.APPLICATION_RECEIVED,
                "Nouvelle candidature reçue", message, candidateId, applicationId);
        createRoleNotification(eventId, "MANAGER", NotificationType.APPLICATION_RECEIVED,
                "Nouvelle candidature reçue", message, candidateId, applicationId);
    }

    private void createApplicationUpdatedNotifications(String eventId, JsonNode event) {
        Long applicationId = longValue(event, "applicationId");
        Long candidateId = longValue(event, "candidateId");
        String status = text(event, "status");
        String message = "La candidature #" + applicationId + " a été mise à jour. Nouveau statut : " + status + ".";
        createRoleNotification(eventId, "HR", NotificationType.APPLICATION_UPDATED,
                "Candidature mise à jour", message, candidateId, applicationId);
        createRoleNotification(eventId, "MANAGER", NotificationType.APPLICATION_UPDATED,
                "Candidature mise à jour", message, candidateId, applicationId);
        String candidateKeycloakId = text(event, "candidateKeycloakId");
        if (candidateKeycloakId != null) {
            createPersonalNotification(eventId, candidateKeycloakId, NotificationType.APPLICATION_UPDATED,
                    "Mise à jour de votre candidature", message, candidateId, applicationId);
        }
    }

    private void createJobOfferNotifications(String eventId, JsonNode event, boolean updated) {
        Long jobOfferId = longValue(event, "jobOfferId");
        String title = text(event, "title");
        String location = text(event, "location");
        String subject = updated ? "Offre d’emploi mise à jour" : "Nouvelle offre d’emploi";
        String message = updated
                ? "L’offre « " + title + " » a été mise à jour."
                : "Une nouvelle offre « " + title + " » est disponible.";
        if (location != null) {
            message += " Localisation : " + location + ".";
        }
        createRoleNotification(eventId, "CANDIDATE", updated ? NotificationType.JOB_OFFER_UPDATED : NotificationType.JOB_OFFER_CREATED,
                subject, message, null, null);
    }

    private void createRoleNotification(String eventId, String role, NotificationType type, String subject,
                                        String message, Long candidateId, Long applicationId) {
        createIfMissing(eventId + ":role:" + role, Notification.builder()
                .audienceRole(role)
                .candidateId(candidateId)
                .applicationId(applicationId)
                .type(type)
                .subject(subject)
                .message(message)
                .status("DELIVERED")
                .channel("IN_APP")
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void createPersonalNotification(String eventId, String candidateKeycloakId, NotificationType type,
                                            String subject, String message, Long candidateId, Long applicationId) {
        createIfMissing(eventId + ":user:" + candidateKeycloakId, Notification.builder()
                .candidateId(candidateId)
                .candidateKeycloakId(candidateKeycloakId)
                .applicationId(applicationId)
                .type(type)
                .subject(subject)
                .message(message)
                .status("DELIVERED")
                .channel("IN_APP")
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void createIfMissing(String deliveryKey, Notification notification) {
        if (notificationRepository.findByDeliveryKey(deliveryKey).isPresent()) {
            return;
        }
        notification.setDeliveryKey(deliveryKey);
        notificationRepository.save(notification);
    }

    private boolean isVisibleTo(Notification notification, String keycloakId, Set<String> roles) {
        return keycloakId.equals(notification.getCandidateKeycloakId())
                || (notification.getAudienceRole() != null && roles.contains(notification.getAudienceRole()));
    }

    private CandidateNotificationDto toDto(Notification notification, String keycloakId) {
        return notificationReadRepository.findByNotificationIdAndReaderKeycloakId(notification.getId(), keycloakId)
                .map(read -> toDto(notification, read.getReadAt()))
                .orElseGet(() -> toDto(notification, (LocalDateTime) null));
    }

    private CandidateNotificationDto toDto(Notification notification, LocalDateTime readAt) {
        return new CandidateNotificationDto(
                notification.getId(), notification.getApplicationId(), notification.getType(), notification.getSubject(),
                notification.getMessage(), notification.getStatus(), notification.getChannel(), notification.getCreatedAt(), readAt
        );
    }

    private String resolveSubject(SendNotificationRequest request) {
        if (request.subject() != null && !request.subject().isBlank()) {
            return request.subject();
        }
        return switch (request.type()) {
            case REJECTION, APPLICATION_UPDATED -> "Mise à jour de votre candidature";
            case WELCOME -> "Bienvenue dans l’équipe";
            case OFFER_ACCEPTED -> "Votre offre d’emploi";
            case CV_TIMEOUT -> "Délai de correction du CV expiré";
            case CV_REVISION_REQUIRED -> "Révision de CV requise";
            case APPLICATION_RECEIVED -> "Nouvelle candidature reçue";
            case CANDIDATE_REGISTERED -> "Nouveau candidat inscrit";
            case JOB_OFFER_CREATED -> "Nouvelle offre d’emploi";
            case JOB_OFFER_UPDATED -> "Offre d’emploi mise à jour";
            case INTERVIEW_SCHEDULED -> "Votre entretien est programmé";
        };
    }

    private String resolveMessage(SendNotificationRequest request) {
        if (request.message() != null && !request.message().isBlank()) {
            return request.message();
        }
        return "Vous avez une nouvelle notification.";
    }

    private String text(JsonNode event, String field) {
        JsonNode value = event.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private Long longValue(JsonNode event, String field) {
        JsonNode value = event.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

}

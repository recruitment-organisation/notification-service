package recruitment.dev.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import recruitment.dev.notificationservice.dto.NotificationResponse;
import recruitment.dev.notificationservice.dto.SendNotificationRequest;
import recruitment.dev.notificationservice.dto.CandidateNotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface NotificationService {

    NotificationResponse send(SendNotificationRequest request);
    void consumeEvent(JsonNode event);
    Page<CandidateNotificationDto> findMine(String keycloakId, Set<String> roles, Pageable pageable);
    CandidateNotificationDto markAsRead(Long notificationId, String keycloakId, Set<String> roles);
}

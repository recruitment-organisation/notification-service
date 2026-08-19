package recruitment.dev.notificationservice.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recruitment.dev.notificationservice.dto.CandidateNotificationDto;
import recruitment.dev.notificationservice.dto.NotificationResponse;
import recruitment.dev.notificationservice.dto.SendNotificationRequest;
import recruitment.dev.notificationservice.service.NotificationService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationService.send(request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Page<CandidateNotificationDto>> mine(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return ResponseEntity.ok(notificationService.findMine(jwt.getSubject(), roles(jwt), pageable));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<CandidateNotificationDto> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, jwt.getSubject(), roles(jwt)));
    }

    @SuppressWarnings("unchecked")
    private Set<String> roles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access)) {
            return Set.of();
        }
        Object roles = access.get("roles");
        if (!(roles instanceof Collection<?> collection)) {
            return Set.of();
        }
        return collection.stream().filter(String.class::isInstance).map(String.class::cast).collect(java.util.stream.Collectors.toSet());
    }
}

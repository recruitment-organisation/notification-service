package recruitment.dev.notificationservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import recruitment.dev.notificationservice.service.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecruitmentEventListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = {
            "recruitment.candidate.v1",
            "recruitment.application.v1",
            "recruitment.job-offer.v1"
    })
    public void consume(String payload) {
        final JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            log.error("Ignoring malformed recruitment event: {}", payload, exception);
            return;
        }

        notificationService.consumeEvent(event);
    }
}

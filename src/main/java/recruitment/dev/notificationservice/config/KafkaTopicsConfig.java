package recruitment.dev.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic candidateEventsTopic() {
        return TopicBuilder.name("recruitment.candidate.v1").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic applicationEventsTopic() {
        return TopicBuilder.name("recruitment.application.v1").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic jobOfferEventsTopic() {
        return TopicBuilder.name("recruitment.job-offer.v1").partitions(1).replicas(1).build();
    }
}

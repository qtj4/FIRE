package kz.enki.fire.ticket_intake_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.incoming:incoming_tickets}")
    private String incomingTopic;

    @Bean
    public NewTopic incomingTicketsTopic() {
        return TopicBuilder.name(incomingTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

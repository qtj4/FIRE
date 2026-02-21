package kz.enki.fire.ticket_intake_service.producer;

import kz.enki.fire.ticket_intake_service.dto.kafka.EnrichedTicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichedTicketProducer {

    private final KafkaTemplate<String, EnrichedTicketEvent> kafkaTemplate;

    @Value("${app.kafka.topic.incoming:incoming_tickets}")
    private String topic;

    public void sendEnrichedTicketEvent(EnrichedTicketEvent event) {
        log.info("Sending enriched ticket event for ticket ID: {}", event.getEnrichedTicketId());
        kafkaTemplate.send(topic, event);
    }
}

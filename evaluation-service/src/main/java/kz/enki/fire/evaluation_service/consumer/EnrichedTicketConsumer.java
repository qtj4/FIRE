package kz.enki.fire.evaluation_service.consumer;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EnrichedTicketConsumer {

    private final AssignmentService assignmentService;

    @KafkaListener(topics = "${app.kafka.topics.incoming:incoming_tickets}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EnrichedTicketEvent event) {
        if (event == null || event.getEnrichedTicketId() == null) {
            log.warn("Received null or empty event");
            return;
        }
        try {
            assignmentService.assignManager(event.getEnrichedTicketId());
        } catch (Exception e) {
            log.error("Failed to assign manager for ticket {}", event.getEnrichedTicketId(), e);
        }
    }
}

package kz.enki.fire.evaluation_service.consumer;

import kz.enki.fire.evaluation_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.evaluation_service.service.IncomingTicketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class IncomingTicketConsumer {

    private final IncomingTicketHandler incomingTicketHandler;

    @KafkaListener(topics = "${app.kafka.topics.incoming:incoming_tickets}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(IncomingTicketMessage message) {
        if (message == null) {
            log.warn("Received null message");
            return;
        }
        try {
            incomingTicketHandler.processAndAssign(message);
        } catch (Exception e) {
            log.error("Failed to process ticket clientGuid={}", message.getClientGuid(), e);
        }
    }
}

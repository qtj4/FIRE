package kz.enki.fire.ticket_intake_service.producer;

import kz.enki.fire.ticket_intake_service.dto.kafka.IncomingTicketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichedTicketProducer {

    private final KafkaTemplate<String, IncomingTicketMessage> kafkaTemplate;

    @Value("${app.kafka.topic.incoming:incoming_tickets}")
    private String topic;

    public void sendIncomingTicket(IncomingTicketMessage message) {
        log.info("Sending ticket to queue clientGuid={}", message.getClientGuid());
        kafkaTemplate.send(topic, message.getClientGuid() != null ? message.getClientGuid().toString() : null, message);
    }
}

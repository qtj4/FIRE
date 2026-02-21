package kz.enki.fire.evaluation_service.kafka;

import jakarta.validation.Valid;
import kz.enki.fire.evaluation_service.dto.TicketContext;
import kz.enki.fire.evaluation_service.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketConsumer {
    
    private final EvaluationService evaluationService;
    private final KafkaTemplate<String, Object> dltKafkaTemplate;
    
    @KafkaListener(
        topics = "incoming_tickets",
        groupId = "evaluation-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenIncomingTickets(@Valid @Payload TicketContext ticketContext) {
        
        log.info("Received ticket for processing: text='{}', location='{}', language='{}'", 
                ticketContext.getText(), ticketContext.getLocation(), ticketContext.getLanguage());
        
        try {
            evaluationService.processTicket(ticketContext);
            log.info("Ticket processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing ticket from Kafka: {}", ticketContext, e);
            sendToDlt(ticketContext, e.getMessage());
        }
    }
    
    private void sendToDlt(TicketContext ticketContext, String errorMessage) {
        try {
            dltKafkaTemplate.send("incoming_tickets_dlt", ticketContext).get();
            log.info("Successfully sent ticket to DLT");
        } catch (Exception e) {
            log.error("Failed to send ticket to DLT", e);
        }
    }
}

package kz.enki.fire.ticket_intake_service.consumer;

import kz.enki.fire.ticket_intake_service.dto.kafka.AssignmentResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AssignmentResultConsumer {

    @KafkaListener(
            topics = "${app.kafka.topic.assignment-result:final_distribution}",
            groupId = "${spring.kafka.consumer.group-id:ticket-intake-service-group}"
    )
    public void consume(AssignmentResultMessage result) {
        if (result == null) return;
        log.info("Assignment result: clientGuid={}, enrichedId={}, manager={}, office={}, status={}",
                result.getClientGuid(), result.getEnrichedTicketId(),
                result.getAssignedManagerName(), result.getAssignedOfficeName(), result.getStatus());
    }
}

package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.kafka.AssignmentResultMessage;
import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.evaluation_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class IncomingTicketHandler {

    private final EnrichedTicketRepository enrichedTicketRepository;
    private final AssignmentService assignmentService;
    private final EnrichedTicketMapper enrichedTicketMapper;
    private final KafkaTemplate<String, AssignmentResultMessage> kafkaTemplate;

    @Value("${app.kafka.topics.outgoing:final_distribution}")
    private String outgoingTopic;

    @Transactional
    public void processAndAssign(IncomingTicketMessage message) {
        if (message == null || message.getClientGuid() == null) {
            log.warn("Ignoring message without clientGuid");
            return;
        }

        EnrichedTicket ticket = EnrichedTicket.builder()
                .clientGuid(message.getClientGuid())
                .rawTicketId(message.getRawTicketId())
                .type(message.getType())
                .priority(message.getPriority())
                .summary(message.getSummary())
                .language(message.getLanguage())
                .sentiment(message.getSentiment())
                .latitude(message.getLatitude())
                .longitude(message.getLongitude())
                .geoNormalized(message.getGeoNormalized())
                .build();

        EnrichedTicket saved = enrichedTicketRepository.save(ticket);
        EnrichedTicketEvent event = enrichedTicketMapper.toEvent(saved);
        assignmentService.assignManager(event);

        EnrichedTicket assigned = enrichedTicketRepository.findById(saved.getId()).orElse(saved);
        AssignmentResultMessage result = AssignmentResultMessage.builder()
                .clientGuid(message.getClientGuid())
                .rawTicketId(message.getRawTicketId())
                .enrichedTicketId(assigned.getId())
                .assignedManagerId(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getId() : null)
                .assignedManagerName(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getFullName() : null)
                .assignedOfficeId(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getId() : null)
                .assignedOfficeName(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getName() : null)
                .status(assigned.getAssignedManager() != null ? "ASSIGNED" : "UNASSIGNED")
                .build();

        kafkaTemplate.send(outgoingTopic, message.getClientGuid().toString(), result);
        log.info("Processed ticket clientGuid={}, enrichedId={}, status={}", message.getClientGuid(), assigned.getId(), result.getStatus());
    }
}

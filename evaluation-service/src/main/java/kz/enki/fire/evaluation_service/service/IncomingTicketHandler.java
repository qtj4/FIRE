package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.kafka.AssignmentResultMessage;
import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.evaluation_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.model.RawTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import kz.enki.fire.evaluation_service.repository.RawTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class IncomingTicketHandler {

    private final RawTicketRepository rawTicketRepository;
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

        RawTicket rawTicket = resolveRawTicket(message);
        EnrichedTicket saved = upsertEnrichedTicket(rawTicket, message);

        EnrichedTicketEvent event = enrichedTicketMapper.toEvent(saved);
        assignmentService.assignManager(event);

        EnrichedTicket assigned = enrichedTicketRepository.findById(saved.getId()).orElse(saved);
        AssignmentResultMessage result = AssignmentResultMessage.builder()
                .clientGuid(message.getClientGuid())
                .rawTicketId(rawTicket.getId())
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

    private RawTicket resolveRawTicket(IncomingTicketMessage message) {
        if (message.getRawTicketId() != null) {
            return rawTicketRepository.findById(message.getRawTicketId())
                    .orElseGet(() -> rawTicketRepository.findByClientGuid(message.getClientGuid())
                            .orElseGet(() -> rawTicketRepository.save(RawTicket.builder()
                                    .clientGuid(message.getClientGuid())
                                    .build())));
        }
        return rawTicketRepository.findByClientGuid(message.getClientGuid())
                .orElseGet(() -> rawTicketRepository.save(RawTicket.builder()
                        .clientGuid(message.getClientGuid())
                        .build()));
    }

    private EnrichedTicket upsertEnrichedTicket(RawTicket rawTicket, IncomingTicketMessage message) {
        EnrichedTicket ticket = findExisting(rawTicket.getId(), message.getClientGuid())
                .orElseGet(EnrichedTicket::new);
        applyPayload(ticket, rawTicket, message);

        try {
            return enrichedTicketRepository.save(ticket);
        } catch (DataIntegrityViolationException ex) {
            // Unique index protects against duplicate rows during Kafka redelivery/races.
            EnrichedTicket existing = findExisting(rawTicket.getId(), message.getClientGuid())
                    .orElseThrow(() -> ex);
            applyPayload(existing, rawTicket, message);
            log.info(
                    "Deduplicated incoming message by business key clientGuid={}, rawTicketId={}",
                    message.getClientGuid(),
                    rawTicket.getId()
            );
            return enrichedTicketRepository.save(existing);
        }
    }

    private Optional<EnrichedTicket> findExisting(Long rawTicketId, UUID clientGuid) {
        return enrichedTicketRepository.findByRawTicketIdAndClientGuid(rawTicketId, clientGuid)
                .or(() -> enrichedTicketRepository.findByRawTicketId(rawTicketId));
    }

    private static void applyPayload(EnrichedTicket ticket, RawTicket rawTicket, IncomingTicketMessage message) {
        ticket.setRawTicket(rawTicket);
        ticket.setClientGuid(message.getClientGuid());
        ticket.setType(message.getType());
        ticket.setPriority(message.getPriority());
        ticket.setSummary(message.getSummary());
        ticket.setLanguage(message.getLanguage());
        ticket.setSentiment(message.getSentiment());
        ticket.setLatitude(message.getLatitude());
        ticket.setLongitude(message.getLongitude());
        ticket.setGeoNormalized(message.getGeoNormalized());
    }
}

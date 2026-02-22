package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketAssignRequest;
import kz.enki.fire.evaluation_service.dto.response.TicketAssignmentResponse;
import kz.enki.fire.evaluation_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HttpTicketAssignmentService {

    private final EnrichedTicketRepository enrichedTicketRepository;
    private final AssignmentService assignmentService;
    private final EnrichedTicketMapper enrichedTicketMapper;

    @Transactional
    public TicketAssignmentResponse createAndAssign(EnrichedTicketAssignRequest request) {
        EnrichedTicket newTicket = EnrichedTicket.builder()
                .clientGuid(request.getClientGuid())
                .type(request.getType())
                .priority(request.getPriority())
                .summary(request.getSummary())
                .language(request.getLanguage())
                .sentiment(request.getSentiment())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geoNormalized(request.getGeoNormalized())
                .build();

        EnrichedTicket saved = enrichedTicketRepository.save(newTicket);
        EnrichedTicketEvent event = enrichedTicketMapper.toEvent(saved);
        assignmentService.assignManager(event);

        EnrichedTicket assigned = enrichedTicketRepository.findById(saved.getId())
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + saved.getId()));

        return buildResponse(assigned);
    }

    @Transactional
    public TicketAssignmentResponse assignExisting(Long enrichedTicketId) {
        EnrichedTicket existing = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + enrichedTicketId));

        EnrichedTicketEvent event = enrichedTicketMapper.toEvent(existing);
        assignmentService.assignManager(event);

        EnrichedTicket assigned = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found after assignment: " + enrichedTicketId));

        return buildResponse(assigned);
    }

    private TicketAssignmentResponse buildResponse(EnrichedTicket assigned) {
        return TicketAssignmentResponse.builder()
                .enrichedTicketId(assigned.getId())
                .assignedManagerId(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getId() : null)
                .assignedManagerName(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getFullName() : null)
                .assignedOfficeId(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getId() : null)
                .assignedOfficeCode(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getCode() : null)
                .assignedOfficeName(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getName() : null)
                .status(assigned.getAssignedManager() != null ? "ASSIGNED" : "UNASSIGNED")
                .message(assigned.getAssignedManager() != null
                        ? "Ticket assigned successfully"
                        : "No manager matched the ticket criteria")
                .build();
    }
}

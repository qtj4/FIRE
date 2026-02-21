package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketAssignRequest;
import kz.enki.fire.evaluation_service.dto.response.TicketAssignmentResponse;
import kz.enki.fire.evaluation_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.model.RawTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import kz.enki.fire.evaluation_service.repository.RawTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HttpTicketAssignmentService {

    private final RawTicketRepository rawTicketRepository;
    private final EnrichedTicketRepository enrichedTicketRepository;
    private final AssignmentService assignmentService;
    private final EnrichedTicketMapper enrichedTicketMapper;

    @Transactional
    public TicketAssignmentResponse createAndAssign(EnrichedTicketAssignRequest request) {

        Long rawTicketId = request.getRawTicketId() != null
                ? request.getRawTicketId()
                : (request.getRawTicket() != null ? request.getRawTicket().getId() : null);

        if (rawTicketId == null) {
            throw new IllegalArgumentException("rawTicketId is required (either rawTicketId or rawTicket.id)");
        }

        RawTicket rawTicket = rawTicketRepository.findById(rawTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Raw ticket not found: " + rawTicketId));

        // 1. Проверяем, есть ли уже enriched
        EnrichedTicket ticket = enrichedTicketRepository.findByRawTicketId(rawTicketId)
                .orElseGet(() -> {
                    // 2. Если нет — создаём
                    EnrichedTicket newTicket = EnrichedTicket.builder()
                            .rawTicket(rawTicket)
                            .clientGuid(request.getClientGuid() != null ? request.getClientGuid() : rawTicket.getClientGuid())
                            .type(request.getType())
                            .priority(request.getPriority())
                            .summary(request.getSummary())
                            .language(request.getLanguage())
                            .sentiment(request.getSentiment())
                            .latitude(request.getLatitude())
                            .longitude(request.getLongitude())
                            .build();

                    return enrichedTicketRepository.save(newTicket);
                });

        // 3. Делаем назначение (если нужно — можешь добавить проверку, чтобы не переназначать)
        EnrichedTicketEvent event = enrichedTicketMapper.toEvent(ticket);
        assignmentService.assignManager(event);

        EnrichedTicket assigned = enrichedTicketRepository.findById(ticket.getId())
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + ticket.getId()));

        return TicketAssignmentResponse.builder()
                .enrichedTicketId(assigned.getId())
                .assignedManagerId(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getId() : null)
                .assignedManagerName(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getFullName() : null)
                .assignedOfficeId(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getId() : null)
                .assignedOfficeName(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getName() : null)
                .status(assigned.getAssignedManager() != null ? "ASSIGNED" : "UNASSIGNED")
                .message(assigned.getAssignedManager() != null
                        ? "Ticket assigned successfully"
                        : "Assignment completed without manager match")
                .build();
    }

    @Transactional
    public TicketAssignmentResponse assignExisting(Long enrichedTicketId) {
        EnrichedTicket existing = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + enrichedTicketId));

        EnrichedTicketEvent event = enrichedTicketMapper.toEvent(existing);
        assignmentService.assignManager(event);

        EnrichedTicket assigned = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found after assignment: " + enrichedTicketId));

        return TicketAssignmentResponse.builder()
                .enrichedTicketId(assigned.getId())
                .assignedManagerId(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getId() : null)
                .assignedManagerName(assigned.getAssignedManager() != null ? assigned.getAssignedManager().getFullName() : null)
                .assignedOfficeId(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getId() : null)
                .assignedOfficeName(assigned.getAssignedOffice() != null ? assigned.getAssignedOffice().getName() : null)
                .status(assigned.getAssignedManager() != null ? "ASSIGNED" : "UNASSIGNED")
                .message(assigned.getAssignedManager() != null
                        ? "Ticket assigned successfully"
                        : "No manager matched the ticket criteria")
                .build();
    }
}

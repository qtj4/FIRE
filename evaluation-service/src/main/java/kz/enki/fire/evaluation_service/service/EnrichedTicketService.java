package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketCreateRequest;
import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketUpdateRequest;
import kz.enki.fire.evaluation_service.dto.response.EnrichedTicketResponse;
import kz.enki.fire.evaluation_service.dto.response.TicketAssignmentResponse;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrichedTicketService {

    private final EnrichedTicketRepository enrichedTicketRepository;
    private final HttpTicketAssignmentService httpTicketAssignmentService;

    public List<EnrichedTicketResponse> findAll() {
        return enrichedTicketRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public EnrichedTicketResponse findById(Long id) {
        EnrichedTicket ticket = enrichedTicketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + id));
        return toResponse(ticket);
    }

    @Transactional
    public EnrichedTicketResponse create(EnrichedTicketCreateRequest request) {
        if (request.getRawTicketId() == null) {
            throw new IllegalArgumentException("rawTicketId is required");
        }
        enrichedTicketRepository.findByRawTicketId(request.getRawTicketId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Enriched ticket already exists for rawTicketId: " + request.getRawTicketId());
        });

        EnrichedTicket ticket = EnrichedTicket.builder()
                .rawTicketId(request.getRawTicketId())
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
        EnrichedTicket saved = enrichedTicketRepository.save(ticket);
        return toResponse(saved);
    }

    @Transactional
    public EnrichedTicketResponse update(Long id, EnrichedTicketUpdateRequest request) {
        EnrichedTicket ticket = enrichedTicketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + id));
        if (request.getClientGuid() != null) ticket.setClientGuid(request.getClientGuid());
        if (request.getType() != null) ticket.setType(request.getType());
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());
        if (request.getSummary() != null) ticket.setSummary(request.getSummary());
        if (request.getLanguage() != null) ticket.setLanguage(request.getLanguage());
        if (request.getSentiment() != null) ticket.setSentiment(request.getSentiment());
        if (request.getLatitude() != null) ticket.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) ticket.setLongitude(request.getLongitude());
        EnrichedTicket saved = enrichedTicketRepository.save(ticket);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!enrichedTicketRepository.existsById(id)) {
            throw new IllegalArgumentException("Enriched ticket not found: " + id);
        }
        enrichedTicketRepository.deleteById(id);
    }

    public TicketAssignmentResponse assignByClientGuid(UUID clientGuid) {
        EnrichedTicket ticket = enrichedTicketRepository.findByClientGuid(clientGuid)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found for clientGuid: " + clientGuid));
        return httpTicketAssignmentService.assignExisting(ticket.getId());
    }

    private EnrichedTicketResponse toResponse(EnrichedTicket t) {
        return EnrichedTicketResponse.builder()
                .id(t.getId())
                .rawTicketId(t.getRawTicketId())
                .clientGuid(t.getClientGuid())
                .type(t.getType())
                .priority(t.getPriority())
                .summary(t.getSummary())
                .language(t.getLanguage())
                .sentiment(t.getSentiment())
                .latitude(t.getLatitude())
                .longitude(t.getLongitude())
                .assignedOfficeId(t.getAssignedOffice() != null ? t.getAssignedOffice().getId() : null)
                .assignedOfficeName(t.getAssignedOffice() != null ? t.getAssignedOffice().getName() : null)
                .assignedManagerId(t.getAssignedManager() != null ? t.getAssignedManager().getId() : null)
                .assignedManagerName(t.getAssignedManager() != null ? t.getAssignedManager().getFullName() : null)
                .enrichedAt(t.getEnrichedAt())
                .build();
    }
}

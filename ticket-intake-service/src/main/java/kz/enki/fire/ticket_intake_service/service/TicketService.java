package kz.enki.fire.ticket_intake_service.service;

import kz.enki.fire.ticket_intake_service.client.N8nClient;
import kz.enki.fire.ticket_intake_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingResult;
import kz.enki.fire.ticket_intake_service.dto.response.N8nEnrichmentResponse;
import kz.enki.fire.ticket_intake_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.ticket_intake_service.model.EnrichedTicket;
import kz.enki.fire.ticket_intake_service.model.RawTicket;
import kz.enki.fire.ticket_intake_service.producer.EnrichedTicketProducer;
import kz.enki.fire.ticket_intake_service.repository.EnrichedTicketRepository;
import kz.enki.fire.ticket_intake_service.repository.RawTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    private final RawTicketRepository rawTicketRepository;
    private final EnrichedTicketRepository enrichedTicketRepository;
    private final N8nClient n8nClient;
    private final GeocodingService geocodingService;
    private final EnrichedTicketProducer enrichedTicketProducer;
    private final EnrichedTicketMapper enrichedTicketMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");

    @Transactional
    public void processTickets(List<TicketCsvRequest> requests) {
        for (TicketCsvRequest req : requests) {
            try {
                RawTicket rawTicket = RawTicket.builder()
                        .clientGuid(UUID.fromString(req.getClientGuid()))
                        .clientGender(req.getClientGender())
                        .birthDate(parseDate(req.getBirthDate()))
                        .description(req.getDescription())
                        .attachments(req.getAttachments())
                        .clientSegment(req.getClientSegment())
                        .country(req.getCountry())
                        .region(req.getRegion())
                        .city(req.getCity())
                        .street(req.getStreet())
                        .houseNumber(req.getHouseNumber())
                        .build();

                rawTicket = rawTicketRepository.save(rawTicket);

                N8nEnrichmentResponse response = n8nClient.enrichTicket(rawTicket);

                GeocodingResult geoResult = null;
                if (response != null && response.getGeo_normalized() != null) {
                    geoResult = geocodingService.geocode(response.getGeo_normalized());
                }

                EnrichedTicket enrichedTicket = EnrichedTicket.builder()
                        .rawTicket(rawTicket)
                        .clientGuid(rawTicket.getClientGuid())
                        .type(response != null ? response.getType() : null)
                        .priority(response != null ? response.getPriority() : null)
                        .summary(response != null ? response.getSummary() : "Pending enrichment...")
                        .sentiment(response != null ? response.getSentiment() : null)
                        .language(response != null ? response.getLanguage() : null)
                        .latitude(geoResult != null ? geoResult.getLatitude() : null)
                        .longitude(geoResult != null ? geoResult.getLongitude() : null)
                        .build();

                enrichedTicket = enrichedTicketRepository.save(enrichedTicket);

                EnrichedTicketEvent event = enrichedTicketMapper.toEvent(enrichedTicket);
                enrichedTicketProducer.sendEnrichedTicketEvent(event);
            } catch (Exception e) {
                log.error("Failed to process ticket for client GUID: {}", req.getClientGuid(), e);
            }
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}

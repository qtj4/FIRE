package kz.enki.fire.ticket_intake_service.service;

import kz.enki.fire.ticket_intake_service.client.N8nClient;
import kz.enki.fire.ticket_intake_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingResult;
import kz.enki.fire.ticket_intake_service.dto.response.N8nEnrichmentResponse;
import kz.enki.fire.ticket_intake_service.mapper.RawTicketMapper;
import kz.enki.fire.ticket_intake_service.model.EnrichedTicket;
import kz.enki.fire.ticket_intake_service.model.RawTicket;
import kz.enki.fire.ticket_intake_service.producer.EnrichedTicketProducer;
import kz.enki.fire.ticket_intake_service.repository.EnrichedTicketRepository;
import kz.enki.fire.ticket_intake_service.repository.RawTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    private final RawTicketRepository rawTicketRepository;
    private final EnrichedTicketRepository enrichedTicketRepository;
    private final N8nClient n8nClient;
    private final GeocodingService geocodingService;
    private final EnrichedTicketProducer enrichedTicketProducer;
    private final RawTicketMapper rawTicketMapper;

    @Qualifier("ticketTaskExecutor")
    private final Executor ticketTaskExecutor;

    @Transactional
    public List<UUID> processTickets(List<TicketCsvRequest> requests) {
        log.info("Starting parallel processing of {} tickets", requests.size());
        List<UUID> sentClientGuids = new CopyOnWriteArrayList<>();

        List<CompletableFuture<Void>> futures = requests.stream()
                .map(req -> CompletableFuture.runAsync(() -> {
                    UUID guid = processSingleTicketAndReturnClientGuid(req);
                    if (guid != null) sentClientGuids.add(guid);
                }, ticketTaskExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((res, ex) -> {
                    if (ex != null) {
                        log.error("Error during parallel ticket processing", ex);
                    }
                    log.info("Finished parallel processing of all tickets, sent {} to queue", sentClientGuids.size());
                    return res;
                }).join();

        return new ArrayList<>(sentClientGuids);
    }

    /** Обрабатывает один тикет (N8N → геокодинг → Kafka). Возвращает clientGuid при успехе, null при ошибке. */
    private UUID processSingleTicketAndReturnClientGuid(TicketCsvRequest req) {
        try {
            RawTicket rawTicket = saveRawTicket(req);

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

            IncomingTicketMessage message = IncomingTicketMessage.builder()
                    .clientGuid(rawTicket.getClientGuid())
                    .rawTicketId(rawTicket.getId())
                    .type(response != null ? response.getType() : null)
                    .sentiment(response != null ? response.getSentiment() : null)
                    .priority(response != null ? response.getPriority() : null)
                    .language(response != null ? response.getLanguage() : null)
                    .summary(response != null ? response.getSummary() : "Pending enrichment...")
                    .geoNormalized(response != null ? response.getGeo_normalized() : null)
                    .latitude(geoResult != null ? geoResult.getLatitude() : null)
                    .longitude(geoResult != null ? geoResult.getLongitude() : null)
                    .build();
            enrichedTicketProducer.sendIncomingTicket(message);

            log.debug("Successfully sent ticket to queue for client: {}", rawTicket.getClientGuid());
            return rawTicket.getClientGuid();
        } catch (Exception e) {
            log.error("Failed to process ticket for client GUID: {}", req.getClientGuid(), e);
            return null;
        }
    }

    @Transactional
    public RawTicket saveRawTicket(TicketCsvRequest req) {
        RawTicket rawTicket = rawTicketMapper.toEntity(req);
        return rawTicketRepository.save(rawTicket);
    }
}

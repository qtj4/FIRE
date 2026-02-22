package kz.enki.fire.ticket_intake_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.ticket_intake_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.ticket_intake_service.dto.request.PutInQueueRequest;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import kz.enki.fire.ticket_intake_service.dto.response.PutInQueueResponse;
import kz.enki.fire.ticket_intake_service.producer.EnrichedTicketProducer;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/intake")
@RequiredArgsConstructor
@Tag(name = "Ticket Intake", description = "CSV ingestion and очередь для evaluation-service")
public class IntakeController {

    private final CsvParserService csvParserService;
    private final TicketService ticketService;
    private final EnrichedTicketProducer enrichedTicketProducer;

    @PostMapping("/tickets")
    public IntakeResponse postTickets(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, TicketCsvRequest.class, ticketService::processTickets);
    }

    @PostMapping("/queue")
    @Operation(
            summary = "Положить тикет в очередь (тест через Swagger)",
            description = "Принимает JSON в формате обогащения (type, sentiment, priority, language, summary, geo_normalized). " +
                    "Отправляет сообщение в Kafka (incoming_tickets); evaluation-service подхватит и обработает."
    )
    public PutInQueueResponse putInQueue(@RequestBody PutInQueueRequest request) {
        UUID clientGuid = request.getClientGuid() != null ? request.getClientGuid() : UUID.randomUUID();
        IncomingTicketMessage message = IncomingTicketMessage.builder()
                .clientGuid(clientGuid)
                .rawTicketId(null)
                .type(request.getType())
                .sentiment(request.getSentiment())
                .priority(request.getPriority())
                .language(request.getLanguage())
                .summary(request.getSummary())
                .geoNormalized(request.getGeoNormalized())
                .latitude(null)
                .longitude(null)
                .build();
        enrichedTicketProducer.sendIncomingTicket(message);
        return PutInQueueResponse.builder()
                .clientGuid(clientGuid)
                .message("Тикет отправлен в очередь incoming_tickets. evaluation-service обработает и вернёт результат в final_distribution.")
                .build();
    }
}

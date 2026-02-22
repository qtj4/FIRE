package kz.enki.fire.ticket_intake_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.ticket_intake_service.dto.kafka.AssignmentResultMessage;
import kz.enki.fire.ticket_intake_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.ticket_intake_service.dto.request.PutInQueueRequest;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import kz.enki.fire.ticket_intake_service.dto.response.PutInQueueResponse;
import kz.enki.fire.ticket_intake_service.dto.response.TicketProcessingResultDto;
import kz.enki.fire.ticket_intake_service.producer.EnrichedTicketProducer;
import kz.enki.fire.ticket_intake_service.service.AssignmentResultStore;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/intake")
@RequiredArgsConstructor
@Tag(name = "Ticket Intake", description = "CSV ingestion and очередь для evaluation-service")
public class IntakeController {

    private final CsvParserService csvParserService;
    private final TicketService ticketService;
    private final EnrichedTicketProducer enrichedTicketProducer;
    private final AssignmentResultStore assignmentResultStore;

    @PostMapping("/tickets")
    @Operation(summary = "Загрузка CSV тикетов", description = "Парсинг → N8N обогащение → Kafka (incoming_tickets) → evaluation вернёт результат в final_distribution. Результаты можно опрашивать через GET /results.")
    public IntakeResponse postTickets(@RequestParam("file") MultipartFile file) {
        CsvParserService.CsvParseResult<TicketCsvRequest> parseResult = csvParserService.parse(file, TicketCsvRequest.class);
        if ("ERROR".equalsIgnoreCase(parseResult.getStatus())) {
            return IntakeResponse.builder()
                    .status(parseResult.getStatus())
                    .message(parseResult.getMessage())
                    .processedCount(0)
                    .failedCount(parseResult.getFailedCount())
                    .results(List.of())
                    .build();
        }
        if (parseResult.getItems().isEmpty()) {
            return IntakeResponse.builder()
                    .status("SUCCESS")
                    .message("Нет строк для обработки")
                    .processedCount(0)
                    .failedCount(parseResult.getFailedCount())
                    .results(List.of())
                    .build();
        }
        List<UUID> sentGuids = ticketService.processTickets(parseResult.getItems());
        List<TicketProcessingResultDto> results = sentGuids.stream()
                .map(guid -> toResultDto(guid, assignmentResultStore.get(guid).orElse(null)))
                .collect(Collectors.toList());
        return IntakeResponse.builder()
                .status("SUCCESS")
                .message("Тикеты отправлены в очередь. Опрашивайте GET /api/v1/intake/results?clientGuids=... для статуса назначения.")
                .processedCount(results.size())
                .failedCount(parseResult.getFailedCount())
                .results(results)
                .build();
    }

    private static TicketProcessingResultDto toResultDto(UUID clientGuid, AssignmentResultMessage msg) {
        if (msg != null) {
            return TicketProcessingResultDto.builder()
                    .clientGuid(clientGuid)
                    .rawTicketId(msg.getRawTicketId())
                    .enrichedTicketId(msg.getEnrichedTicketId())
                    .status(msg.getStatus() != null ? msg.getStatus() : "ASSIGNED")
                    .assignedOfficeName(msg.getAssignedOfficeName())
                    .assignedManagerName(msg.getAssignedManagerName())
                    .message("Назначен офис и менеджер")
                    .build();
        }
        return TicketProcessingResultDto.builder()
                .clientGuid(clientGuid)
                .status("IN_QUEUE")
                .message("В очереди, ожидание evaluation-service")
                .build();
    }

    @GetMapping("/result/{clientGuid}")
    @Operation(summary = "Результат назначения по clientGuid", description = "После отправки в очередь — опрос этого endpoint. 404 если ещё не обработан.")
    public ResponseEntity<TicketProcessingResultDto> getResult(@PathVariable UUID clientGuid) {
        return assignmentResultStore.get(clientGuid)
                .map(msg -> ResponseEntity.ok(toResultDto(clientGuid, msg)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/results")
    @Operation(summary = "Результаты назначения по списку clientGuids", description = "Для опроса после загрузки CSV. clientGuids через запятую.")
    public List<TicketProcessingResultDto> getResults(@RequestParam String clientGuids) {
        List<UUID> guids = java.util.Arrays.stream(clientGuids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
        List<AssignmentResultMessage> fromStore = assignmentResultStore.getByClientGuids(guids);
        return guids.stream()
                .map(guid -> fromStore.stream().filter(m -> m.getClientGuid().equals(guid)).findFirst()
                        .map(m -> toResultDto(guid, m))
                        .orElse(TicketProcessingResultDto.builder()
                                .clientGuid(guid)
                                .status("IN_QUEUE")
                                .message("В очереди")
                                .build()))
                .collect(Collectors.toList());
    }

    @GetMapping("/results/recent")
    @Operation(summary = "Последние результаты назначения", description = "Возвращает последние assignment results из final_distribution для мониторинга очереди.")
    public List<TicketProcessingResultDto> getRecentResults(@RequestParam(defaultValue = "50") int limit) {
        return assignmentResultStore.getRecent(limit).stream()
                .filter(msg -> msg.getClientGuid() != null)
                .map(msg -> toResultDto(msg.getClientGuid(), msg))
                .toList();
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

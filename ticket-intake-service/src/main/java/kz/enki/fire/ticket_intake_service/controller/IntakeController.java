package kz.enki.fire.ticket_intake_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.ticket_intake_service.client.EvaluationTicketClient;
import kz.enki.fire.ticket_intake_service.dto.kafka.AssignmentResultMessage;
import kz.enki.fire.ticket_intake_service.dto.kafka.IncomingTicketMessage;
import kz.enki.fire.ticket_intake_service.dto.request.PutInQueueRequest;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingLookupResponse;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import kz.enki.fire.ticket_intake_service.dto.response.PutInQueueResponse;
import kz.enki.fire.ticket_intake_service.dto.response.TicketProcessingResultDto;
import kz.enki.fire.ticket_intake_service.producer.EnrichedTicketProducer;
import kz.enki.fire.ticket_intake_service.service.AssignmentResultStore;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.GeocodingService;
import kz.enki.fire.ticket_intake_service.service.IdempotencyService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    private static final String IDEMPOTENCY_SCOPE_TICKETS = "intake-tickets";
    private static final String IDEMPOTENCY_SCOPE_QUEUE = "intake-queue";

    private final CsvParserService csvParserService;
    private final TicketService ticketService;
    private final EnrichedTicketProducer enrichedTicketProducer;
    private final EvaluationTicketClient evaluationTicketClient;
    private final AssignmentResultStore assignmentResultStore;
    private final GeocodingService geocodingService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/tickets")
    @Operation(summary = "Загрузка CSV тикетов", description = "Парсинг → N8N обогащение → Kafka (incoming_tickets) → evaluation вернёт результат в final_distribution. Результаты можно опрашивать через GET /results.")
    public IntakeResponse postTickets(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        String requestHash = idempotencyService.isEnabledForKey(idempotencyKey)
                ? idempotencyService.hashMultipartFile(file)
                : null;
        var cachedResponse = idempotencyService.getCachedResponse(
                IDEMPOTENCY_SCOPE_TICKETS,
                idempotencyKey,
                requestHash,
                IntakeResponse.class
        );
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        CsvParserService.CsvParseResult<TicketCsvRequest> parseResult = csvParserService.parse(file, TicketCsvRequest.class);
        IntakeResponse response;

        if ("ERROR".equalsIgnoreCase(parseResult.getStatus())) {
            response = IntakeResponse.builder()
                    .status(parseResult.getStatus())
                    .message(parseResult.getMessage())
                    .processedCount(0)
                    .failedCount(parseResult.getFailedCount())
                    .results(List.of())
                    .build();
            idempotencyService.cacheResponse(IDEMPOTENCY_SCOPE_TICKETS, idempotencyKey, requestHash, response);
            return response;
        }
        if (parseResult.getItems().isEmpty()) {
            response = IntakeResponse.builder()
                    .status("SUCCESS")
                    .message("Нет строк для обработки")
                    .processedCount(0)
                    .failedCount(parseResult.getFailedCount())
                    .results(List.of())
                    .build();
            idempotencyService.cacheResponse(IDEMPOTENCY_SCOPE_TICKETS, idempotencyKey, requestHash, response);
            return response;
        }

        List<UUID> sentGuids = ticketService.processTickets(parseResult.getItems());
        List<TicketProcessingResultDto> results = sentGuids.stream()
                .map(guid -> toResultDto(guid, assignmentResultStore.get(guid).orElse(null)))
                .collect(Collectors.toList());

        response = IntakeResponse.builder()
                .status("SUCCESS")
                .message("Тикеты отправлены в очередь. Опрашивайте GET /api/v1/intake/results?clientGuids=... для статуса назначения.")
                .processedCount(results.size())
                .failedCount(parseResult.getFailedCount())
                .results(results)
                .build();
        idempotencyService.cacheResponse(IDEMPOTENCY_SCOPE_TICKETS, idempotencyKey, requestHash, response);
        return response;
    }

    @GetMapping("/geocode")
    @Operation(
            summary = "Поиск координат по городу/адресу (2GIS)",
            description = "Прокси на geocoding bridge. Пример: /api/v1/intake/geocode?address=Алматы"
    )
    public ResponseEntity<GeocodingLookupResponse> geocode(@RequestParam String address) {
        GeocodingLookupResponse response = geocodingService.lookup(address);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    private static TicketProcessingResultDto toResultDto(UUID clientGuid, AssignmentResultMessage msg) {
        if (msg != null) {
            String status = msg.getStatus() != null ? msg.getStatus() : "ASSIGNED";
            return TicketProcessingResultDto.builder()
                    .clientGuid(clientGuid)
                    .rawTicketId(msg.getRawTicketId())
                    .enrichedTicketId(msg.getEnrichedTicketId())
                    .status(status)
                    .assignedOfficeName(msg.getAssignedOfficeName())
                    .assignedManagerName(msg.getAssignedManagerName())
                    .message(resolveStatusMessage(status))
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
    public PutInQueueResponse putInQueue(
            @RequestBody PutInQueueRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        String requestHash = idempotencyService.isEnabledForKey(idempotencyKey)
                ? idempotencyService.hashObject(request)
                : null;
        var cachedResponse = idempotencyService.getCachedResponse(
                IDEMPOTENCY_SCOPE_QUEUE,
                idempotencyKey,
                requestHash,
                PutInQueueResponse.class
        );
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

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
        evaluationTicketClient.createForImmediateUi(clientGuid, request);
        enrichedTicketProducer.sendIncomingTicket(message);
        assignmentResultStore.put(AssignmentResultMessage.builder()
                .clientGuid(clientGuid)
                .status("IN_QUEUE")
                .build());

        PutInQueueResponse response = PutInQueueResponse.builder()
                .clientGuid(clientGuid)
                .message("Тикет отправлен в очередь incoming_tickets. evaluation-service обработает и вернёт результат в final_distribution.")
                .build();
        idempotencyService.cacheResponse(IDEMPOTENCY_SCOPE_QUEUE, idempotencyKey, requestHash, response);
        return response;
    }

    private static String resolveStatusMessage(String status) {
        if (status == null) {
            return "Статус неизвестен";
        }
        return switch (status.toUpperCase()) {
            case "IN_QUEUE" -> "В очереди, ожидание evaluation-service";
            case "UNASSIGNED" -> "Офис/менеджер пока не назначены";
            case "FAILED" -> "Ошибка обработки";
            default -> "Назначен офис и менеджер";
        };
    }
}

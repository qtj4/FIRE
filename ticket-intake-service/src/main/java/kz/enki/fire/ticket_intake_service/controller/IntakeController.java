package kz.enki.fire.ticket_intake_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.TicketIntakeResponse;
import kz.enki.fire.ticket_intake_service.dto.response.TicketProcessingResult;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/intake")
@RequiredArgsConstructor
@Tag(name = "Ticket Intake", description = "CSV ingestion endpoints for offices, managers, and tickets")
public class IntakeController {

    private final CsvParserService csvParserService;
    private final TicketService ticketService;

    @Operation(summary = "Upload tickets CSV")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tickets ingested",
                    content = @Content(schema = @Schema(implementation = TicketIntakeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid CSV")
    })
    @PostMapping(path = "/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketIntakeResponse postTickets(@Parameter(description = "CSV file with raw tickets", required = true)
                                            @RequestParam("file") MultipartFile file) {
        CsvParserService.CsvParseResult<TicketCsvRequest> parseResult = csvParserService.parse(file, TicketCsvRequest.class);
        if ("ERROR".equalsIgnoreCase(parseResult.getStatus())) {
            return TicketIntakeResponse.builder()
                    .status(parseResult.getStatus())
                    .message(parseResult.getMessage())
                    .processedCount(0)
                    .failedCount(parseResult.getFailedCount())
                    .build();
        }

        List<TicketProcessingResult> results = ticketService.processTickets(parseResult.getItems());
        long failedProcessing = results.stream().filter(r -> "FAILED".equalsIgnoreCase(r.getStatus())).count();

        return TicketIntakeResponse.builder()
                .status("SUCCESS")
                .message("Ticket ingestion and assignment pipeline executed")
                .processedCount(results.size())
                .failedCount(parseResult.getFailedCount() + (int) failedProcessing)
                .results(results)
                .build();
    }
}

package kz.enki.fire.ticket_intake_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.ticket_intake_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.ManagerService;
import kz.enki.fire.ticket_intake_service.service.OfficeService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/intake")
@RequiredArgsConstructor
@Tag(name = "Ticket Intake", description = "CSV ingestion endpoints for offices, managers, and tickets")
public class IntakeController {

    private final CsvParserService csvParserService;
    private final OfficeService officeService;
    private final ManagerService managerService;
    private final TicketService ticketService;

    @Operation(summary = "Upload offices CSV")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offices imported",
                    content = @Content(schema = @Schema(implementation = IntakeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid CSV")
    })
    @PostMapping(path = "/offices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IntakeResponse postOffices(@Parameter(description = "CSV file with offices", required = true)
                                      @RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeService::saveOffices);
    }

    @Operation(summary = "Upload managers CSV")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Managers imported",
                    content = @Content(schema = @Schema(implementation = IntakeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid CSV")
    })
    @PostMapping(path = "/managers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IntakeResponse postManagers(@Parameter(description = "CSV file with managers", required = true)
                                       @RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, ManagerCsvRequest.class, managerService::saveManagers);
    }

    @Operation(summary = "Upload tickets CSV")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tickets ingested",
                    content = @Content(schema = @Schema(implementation = IntakeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid CSV")
    })
    @PostMapping(path = "/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IntakeResponse postTickets(@Parameter(description = "CSV file with raw tickets", required = true)
                                      @RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, TicketCsvRequest.class, ticketService::processTickets);
    }
}

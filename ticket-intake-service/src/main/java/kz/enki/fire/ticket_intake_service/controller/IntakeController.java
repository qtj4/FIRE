package kz.enki.fire.ticket_intake_service.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
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
    private final TicketService ticketService;

    @PostMapping("/tickets")
    public IntakeResponse postTickets(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, TicketCsvRequest.class, ticketService::processTickets);
    }
}

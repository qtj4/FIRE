package kz.enki.fire.ticket_intake_service.controller;

import kz.enki.fire.ticket_intake_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import kz.enki.fire.ticket_intake_service.service.CsvParserService;
import kz.enki.fire.ticket_intake_service.service.ManagerService;
import kz.enki.fire.ticket_intake_service.service.OfficeService;
import kz.enki.fire.ticket_intake_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/intake")
@RequiredArgsConstructor
public class IntakeController {

    private final CsvParserService csvParserService;
    private final OfficeService officeService;
    private final ManagerService managerService;
    private final TicketService ticketService;

    @PostMapping("/offices")
    public IntakeResponse postOffices(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeService::saveOffices);
    }

    @PostMapping("/managers")
    public IntakeResponse postManagers(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, ManagerCsvRequest.class, managerService::saveManagers);
    }

    @PostMapping("/tickets")
    public IntakeResponse postTickets(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, TicketCsvRequest.class, ticketService::processTickets);
    }
}

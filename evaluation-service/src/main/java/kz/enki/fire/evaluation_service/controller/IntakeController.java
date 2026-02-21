package kz.enki.fire.evaluation_service.controller;

import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketAssignRequest;
import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.dto.response.IntakeResponse;
import kz.enki.fire.evaluation_service.dto.response.TicketAssignmentResponse;
import kz.enki.fire.evaluation_service.service.CsvParserService;
import kz.enki.fire.evaluation_service.service.HttpTicketAssignmentService;
import kz.enki.fire.evaluation_service.service.ManagerService;
import kz.enki.fire.evaluation_service.service.OfficeService;
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
    private final HttpTicketAssignmentService httpTicketAssignmentService;

    @PostMapping("/offices")
    public IntakeResponse postOffices(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeService::saveOffices);
    }

    @PostMapping("/managers")
    public IntakeResponse postManagers(@RequestParam("file") MultipartFile file) {
        return csvParserService.parseAndProcess(file, ManagerCsvRequest.class, managerService::saveManagers);
    }

    @PostMapping("/tickets/assign")
    public TicketAssignmentResponse createAndAssignTicket(@RequestBody EnrichedTicketAssignRequest request) {
        return httpTicketAssignmentService.createAndAssign(request);
    }
}

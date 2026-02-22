package kz.enki.fire.evaluation_service.controller;

import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketAssignRequest;
import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketCreateRequest;
import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketUpdateRequest;
import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.dto.response.EnrichedTicketResponse;
import kz.enki.fire.evaluation_service.dto.response.IntakeResponse;
import kz.enki.fire.evaluation_service.dto.response.TicketAssignmentResponse;
import kz.enki.fire.evaluation_service.service.CsvParserService;
import kz.enki.fire.evaluation_service.service.EnrichedTicketService;
import kz.enki.fire.evaluation_service.service.HttpTicketAssignmentService;
import kz.enki.fire.evaluation_service.service.ManagerService;
import kz.enki.fire.evaluation_service.service.OfficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/intake", "/api/evaluation/intake"})
@RequiredArgsConstructor
public class IntakeController {

    private final CsvParserService csvParserService;
    private final OfficeService officeService;
    private final ManagerService managerService;
    private final HttpTicketAssignmentService httpTicketAssignmentService;
    private final EnrichedTicketService enrichedTicketService;

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

    @PostMapping("/tickets/{enrichedTicketId}/assign")
    public TicketAssignmentResponse assignExistingTicket(@PathVariable Long enrichedTicketId) {
        return httpTicketAssignmentService.assignExisting(enrichedTicketId);
    }

    @PostMapping("/tickets/by-client/{clientGuid}/assign")
    public TicketAssignmentResponse assignByClientGuid(@PathVariable UUID clientGuid) {
        return enrichedTicketService.assignByClientGuid(clientGuid);
    }

    // --- CRUD для enriched tickets (тестовые данные и ручное управление) ---

    @GetMapping("/tickets")
    public List<EnrichedTicketResponse> listTickets() {
        return enrichedTicketService.findAll();
    }

    @GetMapping("/tickets/{id}")
    public EnrichedTicketResponse getTicket(@PathVariable Long id) {
        return enrichedTicketService.findById(id);
    }

    @PostMapping("/tickets")
    public EnrichedTicketResponse createTicket(@RequestBody EnrichedTicketCreateRequest request) {
        return enrichedTicketService.create(request);
    }

    @PutMapping("/tickets/{id}")
    public EnrichedTicketResponse updateTicket(@PathVariable Long id, @RequestBody EnrichedTicketUpdateRequest request) {
        return enrichedTicketService.update(id, request);
    }

    @DeleteMapping("/tickets/{id}")
    public void deleteTicket(@PathVariable Long id) {
        enrichedTicketService.delete(id);
    }
}

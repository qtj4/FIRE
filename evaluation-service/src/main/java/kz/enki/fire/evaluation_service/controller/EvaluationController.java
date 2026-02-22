package kz.enki.fire.evaluation_service.controller;

import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketCreateRequest;
import kz.enki.fire.evaluation_service.dto.request.EnrichedTicketUpdateRequest;
import kz.enki.fire.evaluation_service.dto.response.DashboardStatsResponse;
import kz.enki.fire.evaluation_service.dto.response.EnrichedTicketResponse;
import kz.enki.fire.evaluation_service.dto.response.InsightsResponse;
import kz.enki.fire.evaluation_service.dto.response.ServiceHealthResponse;
import kz.enki.fire.evaluation_service.dto.response.TicketAssignmentResponse;
import kz.enki.fire.evaluation_service.service.AnalyticsService;
import kz.enki.fire.evaluation_service.service.EnrichedTicketService;
import kz.enki.fire.evaluation_service.service.HttpTicketAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EnrichedTicketService enrichedTicketService;
    private final HttpTicketAssignmentService httpTicketAssignmentService;
    private final AnalyticsService analyticsService;

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
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        enrichedTicketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/{id}/assign")
    public TicketAssignmentResponse assignExistingTicket(@PathVariable Long id) {
        return httpTicketAssignmentService.assignExisting(id);
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return analyticsService.getDashboardStats();
    }

    @GetMapping("/health")
    public ServiceHealthResponse getHealth() {
        return analyticsService.getServiceHealth();
    }

    @GetMapping("/insights")
    public InsightsResponse getInsights() {
        return analyticsService.getInsights();
    }
}

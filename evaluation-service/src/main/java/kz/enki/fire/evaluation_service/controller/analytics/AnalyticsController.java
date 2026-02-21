package kz.enki.fire.evaluation_service.controller.analytics;

import kz.enki.fire.evaluation_service.dto.response.analytics.AnalyticsTicketResponse;
import kz.enki.fire.evaluation_service.dto.response.analytics.DashboardStatsResponse;
import kz.enki.fire.evaluation_service.dto.response.analytics.InsightsResponse;
import kz.enki.fire.evaluation_service.dto.response.analytics.ServiceHealthResponse;
import kz.enki.fire.evaluation_service.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/tickets")
    public List<AnalyticsTicketResponse> getTickets() {
        return analyticsService.getTickets();
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return analyticsService.getStats();
    }

    @GetMapping("/health")
    public ServiceHealthResponse getHealth() {
        return analyticsService.getHealth();
    }

    @GetMapping("/insights")
    public InsightsResponse getInsights() {
        return analyticsService.getInsights();
    }
}

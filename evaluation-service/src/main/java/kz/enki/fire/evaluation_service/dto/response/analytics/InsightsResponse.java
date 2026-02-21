package kz.enki.fire.evaluation_service.dto.response.analytics;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InsightsResponse {
    String generatedAt;
    List<InsightItem> items;

    @Value
    @Builder
    public static class InsightItem {
        String severity;
        String title;
        String detail;
    }
}

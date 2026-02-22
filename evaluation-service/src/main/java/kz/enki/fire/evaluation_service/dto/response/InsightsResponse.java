package kz.enki.fire.evaluation_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsResponse {
    private String generatedAt;
    private List<InsightItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightItem {
        private String severity;
        private String title;
        private String detail;
    }
}

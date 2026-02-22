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
public class DashboardStatsResponse {
    private Totals totals;
    private List<CityCount> byCity;
    private List<TypeCount> byType;
    private List<OfficeCount> byOffice;
    private List<SentimentCount> bySentiment;
    private List<LanguageCount> byLanguage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private long tickets;
        private double avgPriority;
        private double vipShare;
        private long inRouting;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityCount {
        private String city;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCount {
        private String type;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfficeCount {
        private String office;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentCount {
        private String sentiment;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageCount {
        private String language;
        private long count;
    }
}

package kz.enki.fire.evaluation_service.dto.response.analytics;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DashboardStatsResponse {
    Totals totals;
    List<CityCount> byCity;
    List<TypeCount> byType;
    List<OfficeCount> byOffice;
    List<SentimentCount> bySentiment;
    List<LanguageCount> byLanguage;

    @Value
    @Builder
    public static class Totals {
        long tickets;
        double avgPriority;
        double vipShare;
        long inRouting;
    }

    @Value
    @Builder
    public static class CityCount {
        String city;
        long count;
    }

    @Value
    @Builder
    public static class TypeCount {
        String type;
        long count;
    }

    @Value
    @Builder
    public static class OfficeCount {
        String office;
        long count;
    }

    @Value
    @Builder
    public static class SentimentCount {
        String sentiment;
        long count;
    }

    @Value
    @Builder
    public static class LanguageCount {
        String language;
        long count;
    }
}

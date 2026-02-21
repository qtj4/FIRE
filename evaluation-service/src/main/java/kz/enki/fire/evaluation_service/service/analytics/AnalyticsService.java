package kz.enki.fire.evaluation_service.service.analytics;

import kz.enki.fire.evaluation_service.dto.response.analytics.AnalyticsTicketResponse;
import kz.enki.fire.evaluation_service.dto.response.analytics.DashboardStatsResponse;
import kz.enki.fire.evaluation_service.dto.response.analytics.InsightsResponse;
import kz.enki.fire.evaluation_service.dto.response.analytics.ServiceHealthResponse;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.model.RawTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EnrichedTicketRepository enrichedTicketRepository;

    public List<AnalyticsTicketResponse> getTickets() {
        return enrichedTicketRepository.findAll().stream()
                .sorted(Comparator.comparing(EnrichedTicket::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .map(this::toTicketResponse)
                .toList();
    }

    public DashboardStatsResponse getStats() {
        List<AnalyticsTicketResponse> tickets = getTickets();
        long total = tickets.size();

        double avgPriority = tickets.stream()
                .map(AnalyticsTicketResponse::getPriority)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        long vipCount = tickets.stream()
                .filter(t -> "VIP".equalsIgnoreCase(nullSafe(t.getSegment())))
                .count();

        long inRouting = tickets.stream()
                .filter(t -> isBlank(t.getAssignedManager()))
                .count();

        return DashboardStatsResponse.builder()
                .totals(DashboardStatsResponse.Totals.builder()
                        .tickets(total)
                        .avgPriority(round1(avgPriority))
                        .vipShare(total == 0 ? 0.0 : (double) vipCount / total)
                        .inRouting(inRouting)
                        .build())
                .byCity(groupBy(tickets, AnalyticsTicketResponse::getCity).entrySet().stream()
                        .map(e -> DashboardStatsResponse.CityCount.builder().city(e.getKey()).count(e.getValue()).build())
                        .toList())
                .byType(groupBy(tickets, AnalyticsTicketResponse::getType).entrySet().stream()
                        .map(e -> DashboardStatsResponse.TypeCount.builder().type(e.getKey()).count(e.getValue()).build())
                        .toList())
                .byOffice(groupBy(tickets, t -> defaultIfBlank(t.getOffice(), "Без офиса")).entrySet().stream()
                        .map(e -> DashboardStatsResponse.OfficeCount.builder().office(e.getKey()).count(e.getValue()).build())
                        .toList())
                .bySentiment(groupBy(tickets, t -> defaultIfBlank(t.getSentiment(), "Нейтральный")).entrySet().stream()
                        .map(e -> DashboardStatsResponse.SentimentCount.builder().sentiment(e.getKey()).count(e.getValue()).build())
                        .toList())
                .byLanguage(groupBy(tickets, t -> defaultIfBlank(normalizeLanguage(t.getLanguage()), "RU")).entrySet().stream()
                        .map(e -> DashboardStatsResponse.LanguageCount.builder().language(e.getKey()).count(e.getValue()).build())
                        .toList())
                .build();
    }

    public ServiceHealthResponse getHealth() {
        List<AnalyticsTicketResponse> tickets = getTickets();
        long assigned = tickets.stream().filter(t -> !isBlank(t.getAssignedManager())).count();
        long unassigned = tickets.size() - assigned;
        long highPriorityUnassigned = tickets.stream()
                .filter(t -> isBlank(t.getAssignedManager()))
                .filter(t -> t.getPriority() != null && t.getPriority() >= 8)
                .count();

        return ServiceHealthResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now().toString())
                .ticketsTotal(tickets.size())
                .assignedTotal(assigned)
                .unassignedTotal(unassigned)
                .highPriorityUnassigned(highPriorityUnassigned)
                .build();
    }

    public InsightsResponse getInsights() {
        DashboardStatsResponse stats = getStats();
        long total = stats.getTotals().getTickets();
        long unassigned = stats.getTotals().getInRouting();
        double unassignedRate = total == 0 ? 0.0 : (double) unassigned / total;
        long highPriorityUnassigned = getHealth().getHighPriorityUnassigned();

        List<InsightsResponse.InsightItem> items = List.of(
                buildRoutingInsight(unassignedRate, unassigned),
                buildPriorityInsight(highPriorityUnassigned),
                buildLanguageInsight(stats)
        );

        return InsightsResponse.builder()
                .generatedAt(LocalDateTime.now().toString())
                .items(items)
                .build();
    }

    private AnalyticsTicketResponse toTicketResponse(EnrichedTicket ticket) {
        RawTicket raw = ticket.getRawTicket();

        return AnalyticsTicketResponse.builder()
                .id(ticket.getId() != null ? String.valueOf(ticket.getId()) : null)
                .clientId(raw != null && raw.getClientGuid() != null ? raw.getClientGuid().toString() : null)
                .gender(raw != null ? raw.getClientGender() : null)
                .birthDate(raw != null && raw.getBirthDate() != null ? raw.getBirthDate().toString() : null)
                .segment(defaultIfBlank(raw != null ? raw.getClientSegment() : null, "Mass"))
                .description(raw != null ? raw.getDescription() : null)
                .type(defaultIfBlank(ticket.getType(), "Не определено"))
                .priority(ticket.getPriority() != null ? ticket.getPriority() : 0)
                .attachments(parseAttachments(raw != null ? raw.getAttachments() : null))
                .country(raw != null ? raw.getCountry() : null)
                .region(raw != null ? raw.getRegion() : null)
                .city(raw != null ? raw.getCity() : null)
                .street(raw != null ? raw.getStreet() : null)
                .house(raw != null ? raw.getHouseNumber() : null)
                .office(ticket.getAssignedOffice() != null ? ticket.getAssignedOffice().getName() : null)
                .language(normalizeLanguage(ticket.getLanguage()))
                .sentiment(defaultIfBlank(ticket.getSentiment(), "Нейтральный"))
                .summary(defaultIfBlank(ticket.getSummary(), raw != null ? raw.getDescription() : ""))
                .latitude(ticket.getLatitude() != null ? ticket.getLatitude().doubleValue() : null)
                .longitude(ticket.getLongitude() != null ? ticket.getLongitude().doubleValue() : null)
                .assignedManager(ticket.getAssignedManager() != null ? ticket.getAssignedManager().getFullName() : null)
                .createdAt(raw != null && raw.getCreatedAt() != null ? raw.getCreatedAt().toString() : null)
                .build();
    }

    private List<String> parseAttachments(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String normalizeLanguage(String language) {
        if (isBlank(language)) {
            return "RU";
        }
        String normalized = language.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RU", "KZ", "ENG" -> normalized;
            default -> "RU";
        };
    }

    private Map<String, Long> groupBy(List<AnalyticsTicketResponse> tickets, Function<AnalyticsTicketResponse, String> keyFn) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> defaultIfBlank(keyFn.apply(t), "Не указано"),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private InsightsResponse.InsightItem buildRoutingInsight(double unassignedRate, long unassigned) {
        String severity = unassignedRate > 0.35 ? "high" : (unassignedRate > 0.15 ? "medium" : "low");
        String detail = unassignedRate > 0.35
                ? "Высокая доля обращений без назначения. Проверьте лимиты менеджеров и авто-маршрутизацию."
                : (unassignedRate > 0.15
                ? "Есть накопление в очереди. Стоит перераспределить нагрузку между офисами."
                : "Очередь стабильна, критических задержек не видно.");

        return InsightsResponse.InsightItem.builder()
                .severity(severity)
                .title("Очередь маршрутизации")
                .detail(String.format("%s Сейчас в очереди: %d.", detail, unassigned))
                .build();
    }

    private InsightsResponse.InsightItem buildPriorityInsight(long highPriorityUnassigned) {
        String severity = highPriorityUnassigned > 5 ? "high" : (highPriorityUnassigned > 0 ? "medium" : "low");
        String detail = highPriorityUnassigned > 0
                ? String.format("Не назначено срочных обращений: %d. Рекомендуется выделить VIP/Priority пул.", highPriorityUnassigned)
                : "Все обращения с высоким приоритетом назначены.";

        return InsightsResponse.InsightItem.builder()
                .severity(severity)
                .title("Срочные обращения")
                .detail(detail)
                .build();
    }

    private InsightsResponse.InsightItem buildLanguageInsight(DashboardStatsResponse stats) {
        long total = stats.getTotals().getTickets();
        long nonRu = stats.getByLanguage().stream()
                .filter(l -> !"RU".equalsIgnoreCase(l.getLanguage()))
                .mapToLong(DashboardStatsResponse.LanguageCount::getCount)
                .sum();
        double share = total == 0 ? 0.0 : (double) nonRu / total;

        String severity = share > 0.25 ? "medium" : "low";
        String detail = share > 0.25
                ? "Высокая доля не-RU обращений. Проверьте доступность KZ/ENG менеджеров."
                : "Языковое распределение стабильное.";

        return InsightsResponse.InsightItem.builder()
                .severity(severity)
                .title("Языковая нагрузка")
                .detail(String.format("%s Доля не-RU: %d%%.", detail, (int) Math.round(share * 100)))
                .build();
    }
}

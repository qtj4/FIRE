package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.response.DashboardStatsResponse;
import kz.enki.fire.evaluation_service.dto.response.InsightsResponse;
import kz.enki.fire.evaluation_service.dto.response.ServiceHealthResponse;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EnrichedTicketRepository enrichedTicketRepository;

    public DashboardStatsResponse getDashboardStats() {
        List<EnrichedTicket> tickets = enrichedTicketRepository.findAll();
        long total = tickets.size();
        double avgPriority = tickets.stream()
                .map(EnrichedTicket::getPriority)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        long vipCount = tickets.stream().filter(this::isVip).count();
        long inRouting = tickets.stream().filter(t -> t.getAssignedManager() == null).count();

        return DashboardStatsResponse.builder()
                .totals(DashboardStatsResponse.Totals.builder()
                        .tickets(total)
                        .avgPriority(round(avgPriority))
                        .vipShare(total > 0 ? round((double) vipCount / (double) total) : 0.0)
                        .inRouting(inRouting)
                        .build())
                .byCity(groupAndMap(
                        tickets.stream().map(t -> labelOrDefault(t.getRawTicket() != null ? t.getRawTicket().getCity() : null, "Не указан")),
                        (name, count) -> DashboardStatsResponse.CityCount.builder().city(name).count(count).build()))
                .byType(groupAndMap(
                        tickets.stream().map(t -> labelOrDefault(t.getType(), "Не указан")),
                        (name, count) -> DashboardStatsResponse.TypeCount.builder().type(name).count(count).build()))
                .byOffice(groupAndMap(
                        tickets.stream().map(t -> labelOrDefault(t.getAssignedOffice() != null ? t.getAssignedOffice().getName() : null, "В очереди")),
                        (name, count) -> DashboardStatsResponse.OfficeCount.builder().office(name).count(count).build()))
                .bySentiment(groupAndMap(
                        tickets.stream().map(t -> labelOrDefault(t.getSentiment(), "Не определено")),
                        (name, count) -> DashboardStatsResponse.SentimentCount.builder().sentiment(name).count(count).build()))
                .byLanguage(groupAndMap(
                        tickets.stream().map(t -> labelOrDefault(t.getLanguage(), "UNK")),
                        (name, count) -> DashboardStatsResponse.LanguageCount.builder().language(name).count(count).build()))
                .build();
    }

    public ServiceHealthResponse getServiceHealth() {
        List<EnrichedTicket> tickets = enrichedTicketRepository.findAll();
        long total = tickets.size();
        long assigned = tickets.stream().filter(t -> t.getAssignedManager() != null).count();
        long unassigned = total - assigned;
        long highPriorityUnassigned = tickets.stream()
                .filter(t -> t.getAssignedManager() == null)
                .filter(t -> t.getPriority() != null && t.getPriority() >= 8)
                .count();

        return ServiceHealthResponse.builder()
                .status("UP")
                .timestamp(Instant.now().toString())
                .ticketsTotal(total)
                .assignedTotal(assigned)
                .unassignedTotal(unassigned)
                .highPriorityUnassigned(highPriorityUnassigned)
                .build();
    }

    public InsightsResponse getInsights() {
        List<EnrichedTicket> tickets = enrichedTicketRepository.findAll();
        long total = tickets.size();
        long unassigned = tickets.stream().filter(t -> t.getAssignedManager() == null).count();
        long highPriorityUnassigned = tickets.stream()
                .filter(t -> t.getAssignedManager() == null)
                .filter(t -> t.getPriority() != null && t.getPriority() >= 8)
                .count();

        Map<String, Long> officeLoad = tickets.stream()
                .filter(t -> t.getAssignedOffice() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedOffice().getName(), Collectors.counting()));
        long maxOfficeLoad = officeLoad.values().stream().mapToLong(Long::longValue).max().orElse(0);
        long minOfficeLoad = officeLoad.values().stream().mapToLong(Long::longValue).min().orElse(0);

        List<InsightsResponse.InsightItem> items = new ArrayList<>();

        if (highPriorityUnassigned > 0) {
            items.add(InsightsResponse.InsightItem.builder()
                    .severity("high")
                    .title("Срочные обращения в очереди")
                    .detail("Нераспределенных high-priority тикетов: " + highPriorityUnassigned + ". Проверьте доступность менеджеров VIP-навыков.")
                    .build());
        }

        if (unassigned > 0) {
            double share = total > 0 ? (double) unassigned / (double) total : 0.0;
            String severity = share > 0.25 ? "high" : "medium";
            items.add(InsightsResponse.InsightItem.builder()
                    .severity(severity)
                    .title("Нагрузка на очередь маршрутизации")
                    .detail("В очереди без назначения: " + unassigned + " из " + total + " (" + Math.round(share * 100) + "%).")
                    .build());
        }

        if (maxOfficeLoad > 0 && minOfficeLoad > 0 && maxOfficeLoad >= minOfficeLoad * 2) {
            items.add(InsightsResponse.InsightItem.builder()
                    .severity("medium")
                    .title("Дисбаланс нагрузки по офисам")
                    .detail("Один из офисов обрабатывает минимум в 2 раза больше тикетов. Рекомендуется ребалансировка.")
                    .build());
        }

        if (items.isEmpty()) {
            items.add(InsightsResponse.InsightItem.builder()
                    .severity("low")
                    .title("Система работает стабильно")
                    .detail("Очередь и назначение менеджеров в пределах нормы.")
                    .build());
        }

        return InsightsResponse.builder()
                .generatedAt(Instant.now().toString())
                .items(items)
                .build();
    }

    private boolean isVip(EnrichedTicket ticket) {
        String segment = ticket.getRawTicket() != null ? ticket.getRawTicket().getClientSegment() : null;
        String type = ticket.getType() != null ? ticket.getType().toLowerCase() : "";
        Integer priority = ticket.getPriority();
        return "vip".equalsIgnoreCase(segment) || type.contains("vip") || (priority != null && priority >= 8);
    }

    private String labelOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        return value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private <T> List<T> groupAndMap(java.util.stream.Stream<String> source,
                                    BiFunction<String, Long, T> mapper) {
        return source.collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> mapper.apply(entry.getKey(), entry.getValue()))
                .toList();
    }
}

package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.mapper.EnrichedTicketMapper;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import kz.enki.fire.evaluation_service.model.Manager;
import kz.enki.fire.evaluation_service.model.Office;
import kz.enki.fire.evaluation_service.repository.EnrichedTicketRepository;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import kz.enki.fire.evaluation_service.repository.OfficeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final EnrichedTicketRepository enrichedTicketRepository;
    private final OfficeRepository officeRepository;
    private final ManagerRepository managerRepository;
    private final EnrichedTicketMapper enrichedTicketMapper;
    private final AtomicLong astanaAlmatyCounter = new AtomicLong(0);

    private static final Set<String> ASTANA_ALIASES = Set.of("астана", "astana", "нур-султан", "nur-sultan");
    private static final Set<String> ALMATY_ALIASES = Set.of("алматы", "almaty", "алма-ата", "alma-ata");
    private static final Set<String> KAZAKHSTAN_ALIASES = Set.of(
            "казахстан",
            "қазақстан",
            "республика казахстан",
            "republic of kazakhstan",
            "kz",
            "kazakhstan"
    );

    @Transactional
    public void assignManager(Long enrichedTicketId) {
        EnrichedTicket ticket = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + enrichedTicketId));
        assignManager(enrichedTicketMapper.toEvent(ticket));
    }

    @Transactional
    public void assignManager(EnrichedTicketEvent event) {
        Long enrichedTicketId = event.getEnrichedTicketId();
        EnrichedTicket ticket = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseGet(() -> {
                    log.info("Ticket {} not found, creating from event (CQRS projection)", enrichedTicketId);
                    return enrichedTicketMapper.toEntity(event);
                });

        if (ticket.getAssignedManager() != null) {
            log.info("Ticket {} already assigned to manager {}", enrichedTicketId, ticket.getAssignedManager().getFullName());
            return;
        }

        Office office = selectOffice(ticket);
        if (office == null) {
            log.warn("No office found for ticket {}, fallback to global manager pool", enrichedTicketId);
        }

        List<Manager> managerPool = findManagerPool(office);
        if (managerPool.isEmpty()) {
            log.warn("No managers available for ticket {}", enrichedTicketId);
            return;
        }

        List<Manager> candidates = filterManagersBySkills(ticket, managerPool);
        if (candidates.isEmpty()) {
            String lang = ticket.getLanguage() != null ? ticket.getLanguage().toUpperCase() : "RU";
            candidates = filterManagersByLanguage(managerPool, lang);
            if (candidates.isEmpty()) {
                log.warn(
                        "No strict/relaxed manager match for ticket {}, assigning by load from pool size={}",
                        enrichedTicketId,
                        managerPool.size()
                );
                candidates = managerPool;
            }
        }

        Manager manager = selectByRoundRobin(candidates);
        ticket.setAssignedOffice(resolveAssignedOffice(office, manager));
        ticket.setAssignedManager(manager);
        enrichedTicketRepository.save(ticket);

        manager.setActiveTicketsCount((manager.getActiveTicketsCount() != null ? manager.getActiveTicketsCount() : 0) + 1);
        managerRepository.save(manager);

        log.info(
                "Assigned ticket {} to manager {} in office {}",
                enrichedTicketId,
                manager.getFullName(),
                ticket.getAssignedOffice() != null ? ticket.getAssignedOffice().getName() : "UNKNOWN"
        );
    }

    private Office selectOffice(EnrichedTicket ticket) {
        List<Office> offices = officeRepository.findAll();
        if (offices.isEmpty()) {
            return null;
        }

        if (ticket.getLatitude() != null && ticket.getLongitude() != null) {
            Office nearestByCoordinates = offices.stream()
                    .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                    .min(Comparator.comparing(o -> distance(ticket.getLatitude(), ticket.getLongitude(), o.getLatitude(), o.getLongitude())))
                    .orElse(null);
            if (nearestByCoordinates != null) {
                return nearestByCoordinates;
            }
        }

        Office byGeoNormalized = selectOfficeByGeoNormalized(ticket.getGeoNormalized(), offices);
        if (byGeoNormalized != null) {
            return byGeoNormalized;
        }

        if (shouldSplitByRule(ticket)) {
            return splitUnknownAddress(offices);
        }

        log.warn(
                "Ticket {} has no coordinates; fallback to 50/50 between Astana and Almaty",
                ticket.getId()
        );
        return splitUnknownAddress(offices);
    }

    private Office splitUnknownAddress(List<Office> offices) {
        if (offices.isEmpty()) {
            return null;
        }

        List<Office> astanaOffices = offices.stream()
                .filter(o -> isAstana(o.getName()))
                .sorted(Comparator.comparing(Office::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<Office> almatyOffices = offices.stream()
                .filter(o -> isAlmaty(o.getName()))
                .sorted(Comparator.comparing(Office::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (!astanaOffices.isEmpty() && !almatyOffices.isEmpty()) {
            long turn = astanaAlmatyCounter.getAndIncrement();
            boolean pickAstana = turn % 2 == 0;
            List<Office> cityPool = pickAstana ? astanaOffices : almatyOffices;
            int cityIndex = (int) ((turn / 2) % cityPool.size());
            return cityPool.get(cityIndex);
        }
        if (!astanaOffices.isEmpty()) {
            long turn = astanaAlmatyCounter.getAndIncrement();
            return astanaOffices.get((int) (turn % astanaOffices.size()));
        }
        if (!almatyOffices.isEmpty()) {
            long turn = astanaAlmatyCounter.getAndIncrement();
            return almatyOffices.get((int) (turn % almatyOffices.size()));
        }

        return offices.get((int) (Math.abs(astanaAlmatyCounter.getAndIncrement()) % offices.size()));
    }

    private boolean shouldSplitByRule(EnrichedTicket ticket) {
        if (ticket == null) {
            return true;
        }

        String country = ticket.getRawTicket() != null ? ticket.getRawTicket().getCountry() : null;
        if (isForeignCountry(country)) {
            log.info("Applying 50/50 split for foreign ticket country={}", country);
            return true;
        }

        if (isUnknownAddress(ticket)) {
            log.info("Applying 50/50 split for ticket with unknown address, clientGuid={}", ticket.getClientGuid());
            return true;
        }
        return false;
    }

    private boolean isUnknownAddress(EnrichedTicket ticket) {
        if (ticket == null) {
            return true;
        }
        if (ticket.getLatitude() != null && ticket.getLongitude() != null) {
            return false;
        }
        if (!isBlank(ticket.getGeoNormalized())) {
            return false;
        }
        if (ticket.getRawTicket() == null) {
            return true;
        }
        return isBlank(ticket.getRawTicket().getCountry())
                && isBlank(ticket.getRawTicket().getRegion())
                && isBlank(ticket.getRawTicket().getCity())
                && isBlank(ticket.getRawTicket().getStreet())
                && isBlank(ticket.getRawTicket().getHouseNumber());
    }

    private boolean isForeignCountry(String country) {
        if (isBlank(country)) {
            return false;
        }
        String normalized = country.toLowerCase().trim();
        return !KAZAKHSTAN_ALIASES.contains(normalized);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAstana(String name) {
        return name != null && ASTANA_ALIASES.contains(name.toLowerCase().trim());
    }

    private boolean isAlmaty(String name) {
        return name != null && ALMATY_ALIASES.contains(name.toLowerCase().trim());
    }

    private List<Manager> filterManagersBySkills(EnrichedTicket ticket, List<Manager> managerPool) {
        String type = ticket.getType() != null ? ticket.getType().toLowerCase() : "";
        Integer priority = ticket.getPriority();
        String lang = ticket.getLanguage() != null ? ticket.getLanguage().toUpperCase() : "RU";

        return managerPool.stream()
                .filter(m -> hasVipIfNeeded(m, type, priority))
                .filter(m -> hasPositionForDataChange(m, type))
                .filter(m -> hasLanguageSkill(m, lang))
                .collect(Collectors.toList());
    }

    private List<Manager> findManagerPool(Office office) {
        List<Manager> allManagers = managerRepository.findAll();
        if (allManagers.isEmpty()) {
            return List.of();
        }

        if (office == null || office.getName() == null || office.getName().isBlank()) {
            return allManagers;
        }

        String expectedOffice = normalizeOffice(office.getName());
        List<Manager> byOffice = allManagers.stream()
                .filter(m -> normalizeOffice(m.getOfficeName()).equals(expectedOffice))
                .collect(Collectors.toList());

        if (!byOffice.isEmpty()) {
            return byOffice;
        }

        log.warn("No managers found for office '{}', fallback to global manager pool", office.getName());
        return allManagers;
    }

    private List<Manager> filterManagersByLanguage(List<Manager> managerPool, String lang) {
        if (lang == null || lang.isBlank() || "RU".equalsIgnoreCase(lang)) {
            return managerPool;
        }
        return managerPool.stream()
                .filter(m -> hasLanguageSkill(m, lang))
                .collect(Collectors.toList());
    }

    private Office selectOfficeByGeoNormalized(String geoNormalized, List<Office> offices) {
        if (geoNormalized == null || geoNormalized.isBlank() || offices == null || offices.isEmpty()) {
            return null;
        }

        String normalizedGeo = normalizeOffice(geoNormalized);
        int bestScore = 0;
        Office bestOffice = null;

        for (Office office : offices) {
            int score = scoreOfficeAgainstGeoNormalized(normalizedGeo, office);
            if (score > bestScore) {
                bestScore = score;
                bestOffice = office;
            }
        }

        if (bestOffice != null && bestScore > 0) {
            log.info(
                    "Selected office '{}' by geo_normalized='{}' with score={}",
                    bestOffice.getName(),
                    geoNormalized,
                    bestScore
            );
            return bestOffice;
        }
        return null;
    }

    private int scoreOfficeAgainstGeoNormalized(String normalizedGeo, Office office) {
        if (office == null) {
            return 0;
        }

        int score = 0;
        String normalizedOfficeName = normalizeOffice(office.getName());
        String normalizedOfficeAddress = normalizeOffice(office.getAddress());

        if (!normalizedOfficeName.isBlank() && normalizedGeo.contains(normalizedOfficeName)) {
            score += 120;
        }
        if (!normalizedOfficeAddress.isBlank() && normalizedGeo.contains(normalizedOfficeAddress)) {
            score += 80;
        }

        score += tokenScore(normalizedGeo, normalizedOfficeName, 4, 18);
        score += tokenScore(normalizedGeo, normalizedOfficeAddress, 5, 6);

        return score;
    }

    private int tokenScore(String haystack, String source, int minTokenLength, int tokenWeight) {
        if (haystack == null || haystack.isBlank() || source == null || source.isBlank()) {
            return 0;
        }

        int score = 0;
        String[] tokens = source.split(" ");
        for (String token : tokens) {
            if (token.length() < minTokenLength) {
                continue;
            }
            if (haystack.contains(token)) {
                score += tokenWeight;
            }
        }
        return score;
    }

    private Office resolveAssignedOffice(Office selectedOffice, Manager manager) {
        if (selectedOffice != null) {
            return selectedOffice;
        }
        if (manager == null || manager.getOfficeName() == null || manager.getOfficeName().isBlank()) {
            return null;
        }

        String expectedOffice = normalizeOffice(manager.getOfficeName());
        return officeRepository.findAll().stream()
                .filter(o -> normalizeOffice(o.getName()).equals(expectedOffice))
                .findFirst()
                .orElse(null);
    }

    private String normalizeOffice(String value) {
        if (value == null) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasVipIfNeeded(Manager m, String type, Integer priority) {
        boolean needsVip = "vip".equals(type) || "претензия".equals(type) || (priority != null && priority >= 8);
        if (!needsVip) return true;
        return m.getSkills() != null && m.getSkills().toUpperCase().contains("VIP");
    }

    private boolean hasPositionForDataChange(Manager m, String type) {
        if (!type.contains("смена") && !type.contains("данн")) return true;
        return m.getPosition() != null && m.getPosition().toLowerCase().contains("глав");
    }

    private boolean hasLanguageSkill(Manager m, String lang) {
        if ("RU".equals(lang)) return true;
        return m.getSkills() != null && m.getSkills().toUpperCase().contains(lang);
    }

    private Manager selectByRoundRobin(List<Manager> candidates) {
        List<Manager> sorted = candidates.stream()
                .sorted(Comparator.comparing(m -> m.getActiveTicketsCount() != null ? m.getActiveTicketsCount() : 0))
                .limit(2)
                .collect(Collectors.toList());
        return sorted.get((int) (System.currentTimeMillis() % sorted.size()));
    }

    private double distance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Double.MAX_VALUE;
        double dlat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dlon = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }
}

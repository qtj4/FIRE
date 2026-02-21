package kz.enki.fire.evaluation_service.service;

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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final EnrichedTicketRepository enrichedTicketRepository;
    private final OfficeRepository officeRepository;
    private final ManagerRepository managerRepository;

    private static final Set<String> ASTANA_ALIASES = Set.of("астана", "astana", "нур-султан", "nur-sultan");
    private static final Set<String> ALMATY_ALIASES = Set.of("алматы", "almaty", "алма-ата", "alma-ata");

    @Transactional
    public void assignManager(Long enrichedTicketId) {
        EnrichedTicket ticket = enrichedTicketRepository.findById(enrichedTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Enriched ticket not found: " + enrichedTicketId));

        if (ticket.getAssignedManager() != null) {
            log.info("Ticket {} already assigned to manager {}", enrichedTicketId, ticket.getAssignedManager().getFullName());
            return;
        }

        Office office = selectOffice(ticket);
        if (office == null) {
            log.warn("No office found for ticket {}", enrichedTicketId);
            return;
        }

        List<Manager> candidates = filterManagersBySkills(ticket, office.getName());
        if (candidates.isEmpty()) {
            log.warn("No suitable managers for ticket {} in office {}", enrichedTicketId, office.getName());
            return;
        }

        Manager manager = selectByRoundRobin(candidates);
        ticket.setAssignedOffice(office);
        ticket.setAssignedManager(manager);
        enrichedTicketRepository.save(ticket);

        manager.setActiveTicketsCount((manager.getActiveTicketsCount() != null ? manager.getActiveTicketsCount() : 0) + 1);
        managerRepository.save(manager);

        log.info("Assigned ticket {} to manager {} in office {}", enrichedTicketId, manager.getFullName(), office.getName());
    }

    private Office selectOffice(EnrichedTicket ticket) {
        List<Office> offices = officeRepository.findAll();

        if (ticket.getLatitude() != null && ticket.getLongitude() != null) {
            return offices.stream()
                    .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                    .min(Comparator.comparing(o -> distance(ticket.getLatitude(), ticket.getLongitude(), o.getLatitude(), o.getLongitude())))
                    .orElse(null);
        }

        return splitUnknownAddress(offices);
    }

    private Office splitUnknownAddress(List<Office> offices) {
        if (offices.isEmpty()) return null;
        long astanaCount = offices.stream().filter(o -> isAstana(o.getName())).count();
        long almatyCount = offices.stream().filter(o -> isAlmaty(o.getName())).count();
        if (astanaCount > 0 && almatyCount > 0) {
            int total = offices.size();
            int idx = (int) (System.currentTimeMillis() % 2);
            return offices.stream()
                    .filter(o -> isAstana(o.getName()) || isAlmaty(o.getName()))
                    .sorted(Comparator.comparing(o -> isAstana(o.getName()) ? 0 : 1))
                    .skip(idx)
                    .findFirst()
                    .orElse(offices.get(0));
        }
        return offices.get(0);
    }

    private boolean isAstana(String name) {
        return name != null && ASTANA_ALIASES.contains(name.toLowerCase().trim());
    }

    private boolean isAlmaty(String name) {
        return name != null && ALMATY_ALIASES.contains(name.toLowerCase().trim());
    }

    private List<Manager> filterManagersBySkills(EnrichedTicket ticket, String officeName) {
        List<Manager> byOffice = managerRepository.findByOfficeName(officeName);
        String type = ticket.getType() != null ? ticket.getType().toLowerCase() : "";
        Integer priority = ticket.getPriority();
        String lang = ticket.getLanguage() != null ? ticket.getLanguage().toUpperCase() : "RU";

        return byOffice.stream()
                .filter(m -> hasVipIfNeeded(m, type, priority))
                .filter(m -> hasPositionForDataChange(m, type))
                .filter(m -> hasLanguageSkill(m, lang))
                .collect(Collectors.toList());
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

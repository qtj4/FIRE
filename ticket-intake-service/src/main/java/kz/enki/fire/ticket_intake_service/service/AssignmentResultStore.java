package kz.enki.fire.ticket_intake_service.service;

import kz.enki.fire.ticket_intake_service.dto.kafka.AssignmentResultMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище результатов назначения из final_distribution (по clientGuid).
 * Фронт опрашивает GET /api/v1/intake/results?clientGuids=...
 */
@Component
public class AssignmentResultStore {

    private final Map<UUID, AssignmentResultMessage> byClientGuid = new ConcurrentHashMap<>();

    public void put(AssignmentResultMessage result) {
        if (result != null && result.getClientGuid() != null) {
            byClientGuid.put(result.getClientGuid(), result);
        }
    }

    public Optional<AssignmentResultMessage> get(UUID clientGuid) {
        return Optional.ofNullable(byClientGuid.get(clientGuid));
    }

    public List<AssignmentResultMessage> getByClientGuids(Collection<UUID> clientGuids) {
        if (clientGuids == null) return List.of();
        return clientGuids.stream()
                .map(byClientGuid::get)
                .filter(Objects::nonNull)
                .toList();
    }
}

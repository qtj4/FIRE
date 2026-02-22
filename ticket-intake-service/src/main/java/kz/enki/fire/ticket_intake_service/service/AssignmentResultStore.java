package kz.enki.fire.ticket_intake_service.service;

import kz.enki.fire.ticket_intake_service.dto.kafka.AssignmentResultMessage;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Хранилище результатов назначения из final_distribution (по clientGuid).
 * Фронт опрашивает GET /api/v1/intake/results?clientGuids=...
 */
@Component
public class AssignmentResultStore {

    private record StoredResult(AssignmentResultMessage message, long sequence) {}

    private final Map<UUID, StoredResult> byClientGuid = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public void put(AssignmentResultMessage result) {
        if (result != null && result.getClientGuid() != null) {
            byClientGuid.put(result.getClientGuid(), new StoredResult(result, sequence.incrementAndGet()));
        }
    }

    public Optional<AssignmentResultMessage> get(UUID clientGuid) {
        return Optional.ofNullable(byClientGuid.get(clientGuid)).map(StoredResult::message);
    }

    public List<AssignmentResultMessage> getByClientGuids(Collection<UUID> clientGuids) {
        if (clientGuids == null) return List.of();
        return clientGuids.stream()
                .map(byClientGuid::get)
                .filter(Objects::nonNull)
                .map(StoredResult::message)
                .toList();
    }

    public List<AssignmentResultMessage> getRecent(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 500));
        return byClientGuid.values().stream()
                .sorted(Comparator.comparingLong(StoredResult::sequence).reversed())
                .limit(normalizedLimit)
                .map(StoredResult::message)
                .toList();
    }
}

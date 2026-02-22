package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.enki.fire.ticket_intake_service.dto.kafka.AssignmentResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Хранилище результатов назначения из final_distribution (по clientGuid).
 * Фронт опрашивает GET /api/v1/intake/results?clientGuids=...
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentResultStore {

    private static final String HASH_KEY = "assignment:by-client-guid";
    private static final String RECENT_KEY = "assignment:recent";
    private static final int MAX_RECENT_STORED = 5000;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private record StoredResult(AssignmentResultMessage message, long sequence) {}

    // Fallback cache на случай, если Redis временно недоступен.
    private final Map<UUID, StoredResult> inMemoryFallback = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public void put(AssignmentResultMessage result) {
        if (result != null && result.getClientGuid() != null) {
            long currentSequence = sequence.incrementAndGet();
            inMemoryFallback.put(result.getClientGuid(), new StoredResult(result, currentSequence));

            try {
                String guid = result.getClientGuid().toString();
                String payload = objectMapper.writeValueAsString(result);

                redisTemplate.opsForHash().put(HASH_KEY, guid, payload);
                redisTemplate.opsForZSet().add(RECENT_KEY, guid, System.currentTimeMillis());

                Long size = redisTemplate.opsForZSet().zCard(RECENT_KEY);
                if (size != null && size > MAX_RECENT_STORED) {
                    long toRemove = size - MAX_RECENT_STORED;
                    redisTemplate.opsForZSet().removeRange(RECENT_KEY, 0, toRemove - 1);
                }
            } catch (Exception e) {
                log.warn("Failed to persist assignment result in Redis for clientGuid={}: {}", result.getClientGuid(), e.getMessage());
            }
        }
    }

    public Optional<AssignmentResultMessage> get(UUID clientGuid) {
        if (clientGuid == null) return Optional.empty();
        String guid = clientGuid.toString();

        try {
            Object payload = redisTemplate.opsForHash().get(HASH_KEY, guid);
            if (payload instanceof String value && !value.isBlank()) {
                return Optional.of(objectMapper.readValue(value, AssignmentResultMessage.class));
            }
        } catch (Exception e) {
            log.warn("Failed to read assignment result from Redis for clientGuid={}: {}", clientGuid, e.getMessage());
        }

        return Optional.ofNullable(inMemoryFallback.get(clientGuid)).map(StoredResult::message);
    }

    public List<AssignmentResultMessage> getByClientGuids(Collection<UUID> clientGuids) {
        if (clientGuids == null) return List.of();
        return clientGuids.stream()
                .map(this::get)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<AssignmentResultMessage> getRecent(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 500));
        try {
            Set<String> recentGuids = redisTemplate.opsForZSet().reverseRange(RECENT_KEY, 0, normalizedLimit - 1);
            if (recentGuids != null && !recentGuids.isEmpty()) {
                return recentGuids.stream()
                        .map(this::uuidSafe)
                        .filter(Objects::nonNull)
                        .map(this::get)
                        .flatMap(Optional::stream)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Failed to read recent assignment results from Redis: {}", e.getMessage());
        }

        return inMemoryFallback.values().stream()
                .sorted(Comparator.comparingLong(StoredResult::sequence).reversed())
                .limit(normalizedLimit)
                .map(StoredResult::message)
                .toList();
    }

    private UUID uuidSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

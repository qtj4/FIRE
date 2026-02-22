package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.enabled:true}")
    private boolean enabled;

    @Value("${app.idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    public boolean isEnabledForKey(String key) {
        return enabled && key != null && !key.isBlank();
    }

    public <T> Optional<T> getCachedResponse(String scope, String key, Class<T> responseType) {
        if (!isEnabledForKey(key)) return Optional.empty();

        try {
            String payload = redisTemplate.opsForValue().get(responseKey(scope, key));
            if (payload == null || payload.isBlank()) return Optional.empty();
            return Optional.of(objectMapper.readValue(payload, responseType));
        } catch (Exception e) {
            log.warn("Failed to read idempotency cache for scope={}, key={}: {}", scope, key, e.getMessage());
            return Optional.empty();
        }
    }

    public void cacheResponse(String scope, String key, Object response) {
        if (!isEnabledForKey(key) || response == null) return;

        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    responseKey(scope, key),
                    payload,
                    Duration.ofSeconds(Math.max(ttlSeconds, 60))
            );
        } catch (Exception e) {
            log.warn("Failed to write idempotency cache for scope={}, key={}: {}", scope, key, e.getMessage());
        }
    }

    private String responseKey(String scope, String key) {
        return "idempotency:" + scope + ":" + key.trim();
    }
}

package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.enki.fire.ticket_intake_service.exception.IdempotencyConflictException;
import kz.enki.fire.ticket_intake_service.model.IdempotencyKey;
import kz.enki.fire.ticket_intake_service.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.enabled:true}")
    private boolean enabled;

    @Value("${app.idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    public boolean isEnabledForKey(String key) {
        return enabled && key != null && !key.isBlank();
    }

    public String hashObject(Object payload) {
        try {
            return sha256(objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize request for idempotency hash", e);
        }
    }

    public String hashMultipartFile(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read multipart payload for idempotency hash", e);
        }
    }

    @Transactional
    public <T> Optional<T> getCachedResponse(String endpoint, String key, String requestHash, Class<T> responseType) {
        if (!isEnabledForKey(key)) return Optional.empty();

        IdempotencyKey stored = idempotencyKeyRepository
                .findByEndpointAndIdempotencyKey(endpoint, normalizeKey(key))
                .orElse(null);
        if (stored == null) return Optional.empty();

        if (stored.getExpiresAt() != null && stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            idempotencyKeyRepository.delete(stored);
            return Optional.empty();
        }

        validateRequestHash(endpoint, key, requestHash, stored.getRequestHash());

        if (stored.getResponsePayload() == null || stored.getResponsePayload().isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(stored.getResponsePayload(), responseType));
        } catch (Exception e) {
            log.warn("Failed to deserialize cached idempotency response endpoint={}, key={}", endpoint, key);
            return Optional.empty();
        }
    }

    @Transactional
    public void cacheResponse(String endpoint, String key, String requestHash, Object response) {
        if (!isEnabledForKey(key) || response == null) return;

        final String normalizedKey = normalizeKey(key);
        final String normalizedEndpoint = endpoint != null ? endpoint.trim() : "";
        final String normalizedRequestHash = normalizeHash(requestHash);

        final String responsePayload;
        try {
            responsePayload = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize response for idempotency storage", e);
        }

        final String responseHash = sha256(responsePayload.getBytes(StandardCharsets.UTF_8));
        final LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(Math.max(ttlSeconds, 60));

        IdempotencyKey existing = idempotencyKeyRepository
                .findByEndpointAndIdempotencyKey(normalizedEndpoint, normalizedKey)
                .orElse(null);

        if (existing != null) {
            validateRequestHash(normalizedEndpoint, normalizedKey, normalizedRequestHash, existing.getRequestHash());
            existing.setResponsePayload(responsePayload);
            existing.setResponseHash(responseHash);
            existing.setExpiresAt(expiresAt);
            idempotencyKeyRepository.save(existing);
            return;
        }

        IdempotencyKey newEntry = IdempotencyKey.builder()
                .endpoint(normalizedEndpoint)
                .idempotencyKey(normalizedKey)
                .requestHash(normalizedRequestHash)
                .responseHash(responseHash)
                .responsePayload(responsePayload)
                .expiresAt(expiresAt)
                .build();

        try {
            idempotencyKeyRepository.save(newEntry);
        } catch (DataIntegrityViolationException ex) {
            IdempotencyKey concurrent = idempotencyKeyRepository
                    .findByEndpointAndIdempotencyKey(normalizedEndpoint, normalizedKey)
                    .orElseThrow(() -> ex);
            validateRequestHash(normalizedEndpoint, normalizedKey, normalizedRequestHash, concurrent.getRequestHash());
            concurrent.setResponsePayload(responsePayload);
            concurrent.setResponseHash(responseHash);
            concurrent.setExpiresAt(expiresAt);
            idempotencyKeyRepository.save(concurrent);
        }
    }

    private static String normalizeKey(String key) {
        return key.trim();
    }

    private static String normalizeHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Request hash is required for idempotency storage");
        }
        return hash.trim();
    }

    private static String sha256(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256 hash", e);
        }
    }

    private static void validateRequestHash(String endpoint, String key, String incomingHash, String storedHash) {
        if (!Objects.equals(normalizeHash(incomingHash), normalizeHash(storedHash))) {
            throw new IdempotencyConflictException(
                    "Idempotency key already used with different payload for endpoint=" + endpoint + ", key=" + key
            );
        }
    }
}

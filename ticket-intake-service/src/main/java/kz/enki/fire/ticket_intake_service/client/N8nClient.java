package kz.enki.fire.ticket_intake_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kz.enki.fire.ticket_intake_service.dto.response.N8nEnrichmentResponse;
import kz.enki.fire.ticket_intake_service.model.RawTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class N8nClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${n8n.url}")
    private String n8nUrl;

    @Value("${n8n.port}")
    private String n8nPort;

    @Value("${n8n.webhook-path}")
    private String webhookPath;

    @Value("${n8n.api-key}")
    private String apiKey;

    @Value("${n8n.max-concurrent-requests:8}")
    private int maxConcurrentRequests;

    @Value("${n8n.max-concurrent-wait-ms:30000}")
    private long maxConcurrentWaitMs;

    private Semaphore n8nSemaphore;

    @PostConstruct
    void initSemaphore() {
        n8nSemaphore = new Semaphore(Math.max(1, maxConcurrentRequests), true);
    }

    public N8nEnrichmentResponse enrichTicket(RawTicket rawTicket) {
        boolean permitAcquired = false;
        String primaryUrl = buildUrl(webhookPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", apiKey);
        headers.set("Accept", "application/json");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("clientGuid", rawTicket.getClientGuid() != null ? rawTicket.getClientGuid().toString() : "");
        body.add("description", Objects.toString(rawTicket.getDescription(), ""));
        body.add("clientSegment", Objects.toString(rawTicket.getClientSegment(), ""));
        body.add("language", "RU");
        body.add("ticket", rawTicket);

        if (rawTicket.getAttachments() != null && !rawTicket.getAttachments().isBlank()) {
            File file = new File(rawTicket.getAttachments());
            if (file.exists()) {
                body.add("attachment", new FileSystemResource(file));
            } else {
                log.warn("Attachment file not found: {}", rawTicket.getAttachments());
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            permitAcquired = n8nSemaphore.tryAcquire(maxConcurrentWaitMs, TimeUnit.MILLISECONDS);
            if (!permitAcquired) {
                log.warn(
                        "n8n concurrency gate timeout: maxConcurrentRequests={}, waitMs={}",
                        maxConcurrentRequests,
                        maxConcurrentWaitMs
                );
                return null;
            }

            if (isTestWebhookPath(webhookPath)) {
                log.warn("Using n8n test webhook path. Ensure workflow is in 'Listen for test event' mode: {}", webhookPath);
            }
            log.info("Calling n8n webhook: {}", primaryUrl);
            N8nEnrichmentResponse response = requestEnrichment(primaryUrl, requestEntity);
            if (response != null) {
                return response;
            }

            if (isTestWebhookPath(webhookPath)) {
                String fallbackPath = webhookPath.replace("/webhook-test/", "/webhook/");
                String fallbackUrl = buildUrl(fallbackPath);
                log.warn("Test webhook returned empty payload. Retrying n8n production webhook: {}", fallbackUrl);
                return requestEnrichment(fallbackUrl, requestEntity);
            }

            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for n8n concurrency gate");
            return null;
        } catch (RestClientResponseException e) {
            log.error("n8n webhook response error: status={}, url={}, body={}",
                    e.getRawStatusCode(), primaryUrl, e.getResponseBodyAsString());

            if (isTestWebhookPath(webhookPath) && e.getRawStatusCode() == 404) {
                String fallbackPath = webhookPath.replace("/webhook-test/", "/webhook/");
                String fallbackUrl = buildUrl(fallbackPath);
                try {
                    log.warn("Test webhook not found. Retrying n8n production webhook: {}", fallbackUrl);
                    return requestEnrichment(fallbackUrl, requestEntity);
                } catch (Exception retryError) {
                    log.error("Retry to production webhook failed: {}", retryError.getMessage(), retryError);
                }
            }
            return null;
        } catch (ResourceAccessException e) {
            log.error("n8n webhook network error: url={}, message={}", primaryUrl, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Error calling n8n webhook: {}", e.getMessage(), e);
            return null;
        } finally {
            if (permitAcquired) {
                n8nSemaphore.release();
            }
        }
    }

    private String buildUrl(String path) {
        String base = n8nUrl == null ? "" : n8nUrl.trim();
        String normalizedPath = path == null ? "" : path.trim();

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        boolean baseHasPort = base.matches("^https?://[^/]+:\\d+$");
        if (!baseHasPort && n8nPort != null && !n8nPort.isBlank()) {
            base = base + ":" + n8nPort.trim();
        }

        return base + normalizedPath;
    }

    private boolean isTestWebhookPath(String path) {
        return path != null && path.contains("/webhook-test/");
    }

    private N8nEnrichmentResponse requestEnrichment(String url, HttpEntity<MultiValueMap<String, Object>> requestEntity) {
        Object rawResponse = restTemplate.postForObject(url, requestEntity, Object.class);
        N8nEnrichmentResponse parsed = parseEnrichmentResponse(rawResponse);
        if (parsed == null) {
            log.warn("n8n returned payload without enrichment fields, url={}", url);
        }
        return parsed;
    }

    private N8nEnrichmentResponse parseEnrichmentResponse(Object rawResponse) {
        if (rawResponse == null) {
            return null;
        }

        JsonNode root;
        if (rawResponse instanceof String rawText) {
            try {
                root = objectMapper.readTree(rawText);
            } catch (Exception ignored) {
                return null;
            }
        } else {
            root = objectMapper.valueToTree(rawResponse);
        }

        JsonNode payload = unwrapPayload(root, 0);
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            return null;
        }

        N8nEnrichmentResponse response = new N8nEnrichmentResponse();
        response.setType(readText(payload, "type", "ticketType", "category"));
        response.setSentiment(readText(payload, "sentiment", "tone"));
        response.setPriority(readInteger(payload, "priority", "priority_score", "priorityScore"));
        response.setLanguage(readText(payload, "language", "lang", "detected_language", "detectedLanguage"));
        response.setSummary(readText(payload, "summary", "description_summary", "shortSummary"));
        response.setGeo_normalized(readText(payload, "geo_normalized", "geoNormalized", "geo"));

        return isEnrichmentEmpty(response) ? null : response;
    }

    private JsonNode unwrapPayload(JsonNode node, int depth) {
        if (node == null || node.isNull() || depth > 6) {
            return node;
        }

        if (node.isArray()) {
            return node.size() == 0 ? node : unwrapPayload(node.get(0), depth + 1);
        }

        if (!node.isObject()) {
            return node;
        }

        if (containsEnrichmentFields(node)) {
            return node;
        }

        String[] wrappers = {"data", "result", "output", "payload", "item", "json", "body"};
        for (String wrapper : wrappers) {
            JsonNode wrapped = findFieldIgnoreCase(node, wrapper);
            if (wrapped != null && !wrapped.isNull()) {
                JsonNode unwrapped = unwrapPayload(wrapped, depth + 1);
                if (unwrapped != null && !unwrapped.isNull() && containsEnrichmentFields(unwrapped)) {
                    return unwrapped;
                }
            }
        }

        if (node.size() == 1) {
            Iterator<JsonNode> iterator = node.elements();
            if (iterator.hasNext()) {
                return unwrapPayload(iterator.next(), depth + 1);
            }
        }

        return node;
    }

    private boolean containsEnrichmentFields(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        return findFieldIgnoreCase(node, "type") != null
                || findFieldIgnoreCase(node, "sentiment") != null
                || findFieldIgnoreCase(node, "priority") != null
                || findFieldIgnoreCase(node, "language") != null
                || findFieldIgnoreCase(node, "summary") != null
                || findFieldIgnoreCase(node, "geo_normalized") != null
                || findFieldIgnoreCase(node, "geoNormalized") != null;
    }

    private String readText(JsonNode node, String... aliases) {
        for (String alias : aliases) {
            JsonNode candidate = findFieldIgnoreCase(node, alias);
            if (candidate == null || candidate.isNull()) {
                continue;
            }
            String value = candidate.asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String... aliases) {
        for (String alias : aliases) {
            JsonNode candidate = findFieldIgnoreCase(node, alias);
            if (candidate == null || candidate.isNull()) {
                continue;
            }
            if (candidate.isInt() || candidate.isLong() || candidate.isShort()) {
                return candidate.intValue();
            }
            if (candidate.isNumber()) {
                return (int) Math.round(candidate.asDouble());
            }
            String raw = candidate.asText(null);
            if (raw != null && !raw.isBlank()) {
                try {
                    return Integer.parseInt(raw.trim());
                } catch (NumberFormatException ignored) {
                    // keep trying aliases
                }
            }
        }
        return null;
    }

    private JsonNode findFieldIgnoreCase(JsonNode node, String field) {
        if (node == null || !node.isObject() || field == null) {
            return null;
        }

        JsonNode direct = node.get(field);
        if (direct != null) {
            return direct;
        }

        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            String current = iterator.next();
            if (field.equalsIgnoreCase(current)) {
                return node.get(current);
            }
        }
        return null;
    }

    private boolean isEnrichmentEmpty(N8nEnrichmentResponse response) {
        if (response == null) {
            return true;
        }
        return isBlank(response.getType())
                && isBlank(response.getSentiment())
                && response.getPriority() == null
                && isBlank(response.getLanguage())
                && isBlank(response.getSummary())
                && isBlank(response.getGeo_normalized());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

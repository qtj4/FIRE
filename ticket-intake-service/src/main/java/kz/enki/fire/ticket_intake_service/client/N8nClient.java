package kz.enki.fire.ticket_intake_service.client;

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
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class N8nClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${n8n.url}")
    private String n8nUrl;

    @Value("${n8n.port}")
    private String n8nPort;

    @Value("${n8n.webhook-path}")
    private String webhookPath;

    @Value("${n8n.api-key}")
    private String apiKey;

    public N8nEnrichmentResponse enrichTicket(RawTicket rawTicket) {
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
            if (isTestWebhookPath(webhookPath)) {
                log.warn("Using n8n test webhook path. Ensure workflow is in 'Listen for test event' mode: {}", webhookPath);
            }
            log.info("Calling n8n webhook: {}", primaryUrl);
            return restTemplate.postForObject(primaryUrl, requestEntity, N8nEnrichmentResponse.class);
        } catch (RestClientResponseException e) {
            log.error("n8n webhook response error: status={}, url={}, body={}",
                    e.getRawStatusCode(), primaryUrl, e.getResponseBodyAsString());

            if (isTestWebhookPath(webhookPath) && e.getRawStatusCode() == 404) {
                String fallbackPath = webhookPath.replace("/webhook-test/", "/webhook/");
                String fallbackUrl = buildUrl(fallbackPath);
                try {
                    log.warn("Test webhook not found. Retrying n8n production webhook: {}", fallbackUrl);
                    return restTemplate.postForObject(fallbackUrl, requestEntity, N8nEnrichmentResponse.class);
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
}

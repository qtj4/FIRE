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
import org.springframework.web.client.RestTemplate;

import java.io.File;

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
        String url = String.format("%s:%s%s", n8nUrl, n8nPort, webhookPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", apiKey);
        headers.set("Accept", "application/json");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
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
            return restTemplate.postForObject(url, requestEntity, N8nEnrichmentResponse.class);
        } catch (Exception e) {
            log.error("Error calling n8n webhook: {}", e.getMessage());
            return null;
        }
    }
}

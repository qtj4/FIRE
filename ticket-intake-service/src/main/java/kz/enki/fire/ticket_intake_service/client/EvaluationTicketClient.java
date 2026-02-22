package kz.enki.fire.ticket_intake_service.client;

import kz.enki.fire.ticket_intake_service.dto.request.EvaluationTicketCreateRequest;
import kz.enki.fire.ticket_intake_service.dto.request.PutInQueueRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@Slf4j
public class EvaluationTicketClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${evaluation.base-url}")
    private String evaluationBaseUrl;

    @Value("${evaluation.tickets-path:/api/evaluation/tickets}")
    private String ticketsPath;

    public void createForImmediateUi(UUID clientGuid, PutInQueueRequest request) {
        if (clientGuid == null || request == null) {
            return;
        }

        EvaluationTicketCreateRequest payload = EvaluationTicketCreateRequest.builder()
                .rawTicketId(null)
                .clientGuid(clientGuid)
                .type(request.getType())
                .priority(request.getPriority())
                .summary(request.getSummary())
                .language(request.getLanguage())
                .sentiment(request.getSentiment())
                .latitude(null)
                .longitude(null)
                .build();

        String url = buildUrl();
        try {
            restTemplate.postForObject(url, payload, Object.class);
            log.info("Synced ticket to evaluation-service for immediate UI, clientGuid={}", clientGuid);
        } catch (RestClientResponseException e) {
            log.warn(
                    "Failed to sync ticket to evaluation-service: status={}, clientGuid={}, body={}",
                    e.getRawStatusCode(),
                    clientGuid,
                    e.getResponseBodyAsString()
            );
        } catch (ResourceAccessException e) {
            log.warn(
                    "Evaluation-service is unreachable while syncing ticket clientGuid={}: {}",
                    clientGuid,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.warn("Unexpected error while syncing ticket clientGuid={}: {}", clientGuid, e.getMessage());
        }
    }

    private String buildUrl() {
        String base = evaluationBaseUrl == null ? "" : evaluationBaseUrl.trim();
        String path = ticketsPath == null ? "/api/evaluation/tickets" : ticketsPath.trim();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + path;
    }
}

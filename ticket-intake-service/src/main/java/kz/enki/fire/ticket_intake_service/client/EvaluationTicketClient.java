package kz.enki.fire.ticket_intake_service.client;

import kz.enki.fire.ticket_intake_service.dto.request.EvaluationAssignRequest;
import kz.enki.fire.ticket_intake_service.dto.request.PutInQueueRequest;
import kz.enki.fire.ticket_intake_service.dto.response.EvaluationAssignmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class EvaluationTicketClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${evaluation.base-url}")
    private String evaluationBaseUrl;

    @Value("${evaluation.assign-path:/api/v1/intake}")
    private String assignPath;

    public EvaluationAssignmentResponse assignImmediately(
            Long rawTicketId,
            UUID clientGuid,
            PutInQueueRequest request,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (rawTicketId == null || clientGuid == null || request == null) {
            throw new IllegalArgumentException("rawTicketId, clientGuid and request are required");
        }

        EvaluationAssignRequest payload = EvaluationAssignRequest.builder()
                .rawTicketId(rawTicketId)
                .clientGuid(clientGuid)
                .type(request.getType())
                .priority(request.getPriority())
                .summary(request.getSummary())
                .language(request.getLanguage())
                .sentiment(request.getSentiment())
                .latitude(latitude)
                .longitude(longitude)
                .build();

        String url = buildAssignUrl();
        try {
            EvaluationAssignmentResponse response = restTemplate.postForObject(
                    url,
                    payload,
                    EvaluationAssignmentResponse.class
            );
            if (response == null) {
                throw new IllegalStateException("evaluation-service returned empty assignment response");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Evaluation assign failed: status=" + e.getRawStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    e
            );
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("Evaluation-service is unreachable: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error calling evaluation-service: " + e.getMessage(), e);
        }
    }

    private String buildAssignUrl() {
        String base = evaluationBaseUrl == null ? "" : evaluationBaseUrl.trim();
        String path = assignPath == null ? "/api/v1/intake" : assignPath.trim();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + path + "/tickets/assign";
    }
}

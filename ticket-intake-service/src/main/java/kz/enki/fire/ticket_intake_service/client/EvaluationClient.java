package kz.enki.fire.ticket_intake_service.client;

import kz.enki.fire.ticket_intake_service.dto.response.EvaluationAssignmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${evaluation.base-url:http://localhost:8082}")
    private String evaluationBaseUrl;

    @Value("${evaluation.assign-path:/api/v1/intake}")
    private String assignPath;

    public EvaluationAssignmentResponse assignTicket(Long enrichedTicketId) {
        String url = String.format("%s%s/tickets/%d/assign", evaluationBaseUrl, assignPath, enrichedTicketId);
        try {
            return restTemplate.postForObject(url, HttpEntity.EMPTY, EvaluationAssignmentResponse.class);
        } catch (Exception e) {
            log.error("Failed to request assignment for enrichedTicketId={}", enrichedTicketId, e);
            return null;
        }
    }
}

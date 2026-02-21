package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${geocoding.positionstack.api-key:18d8b4d34ff9e61b974e3d3c2e7ba730}")
    private String apiKey;

    @Value("${geocoding.positionstack.url:http://api.positionstack.com/v1/forward}")
    private String positionstackUrl;

    public GeocodingResult geocode(Map<String, String> addressComponents) {
        String fullAddress = addressComponents.values().stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));

        if (fullAddress.isBlank()) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(positionstackUrl)
                    .queryParam("access_key", apiKey)
                    .queryParam("query", fullAddress)
                    .queryParam("limit", 1)
                    .toUriString();

            log.info("Geocoding via Positionstack: {}", fullAddress);

            ResponseEntity<PositionstackResponse> response = restTemplate.getForEntity(url, PositionstackResponse.class);
            PositionstackResponse body = response.getBody();

            if (body != null && body.getData() != null && !body.getData().isEmpty()) {
                PositionstackData result = body.getData().get(0);
                log.info("Found coordinates for {}: lat={}, lon={}", fullAddress, result.getLatitude(), result.getLongitude());
                return new GeocodingResult(
                        BigDecimal.valueOf(result.getLatitude()),
                        BigDecimal.valueOf(result.getLongitude())
                );
            } else {
                log.warn("No results found for: {}", fullAddress);
            }
        } catch (Exception e) {
            log.error("Geocoding error for '{}': {}", fullAddress, e.getMessage());
        }

        return null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PositionstackResponse {
        private List<PositionstackData> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PositionstackData {
        private double latitude;
        private double longitude;
        private String label;
    }
}

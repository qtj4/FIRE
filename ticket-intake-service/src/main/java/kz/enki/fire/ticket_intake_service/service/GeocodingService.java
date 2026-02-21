package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${geocoding.nominatim.url:https://nominatim.openstreetmap.org/search}")
    private String nominatimUrl;

    @Value("${geocoding.nominatim.user-agent:FIRE-Project-Geocoding-Service}")
    private String userAgent;

    public GeocodingResult geocode(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(nominatimUrl)
                    .queryParam("q", address)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .toUriString();

            log.info("Geocoding address: {}", address);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResult[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    NominatimResult[].class
            );

            NominatimResult[] results = response.getBody();

            if (results != null && results.length > 0) {
                NominatimResult result = results[0];
                return new GeocodingResult(
                        new BigDecimal(result.getLat()),
                        new BigDecimal(result.getLon())
                );
            }
        } catch (Exception e) {
            log.error("Error during geocoding: {}", e.getMessage());
        }

        return null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NominatimResult {
        private String lat;
        private String lon;
        @JsonProperty("display_name")
        private String displayName;
    }
}

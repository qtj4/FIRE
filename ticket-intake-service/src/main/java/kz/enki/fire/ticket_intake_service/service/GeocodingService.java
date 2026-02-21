package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${geocoding.custom.url:http://2.133.130.153:5000/geocode}")
    private String geocodingUrl;

    public GeocodingResult geocode(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(geocodingUrl)
                    .queryParam("address", address)
                    .toUriString();

            log.info("Geocoding via custom server: {}", address);

            ResponseEntity<CustomGeocodingResponse> response = restTemplate.getForEntity(url, CustomGeocodingResponse.class);
            CustomGeocodingResponse body = response.getBody();

            if (body != null) {
                log.info("Found coordinates for {}: lat={}, lon={}", address, body.getLat(), body.getLon());
                return new GeocodingResult(
                        BigDecimal.valueOf(body.getLat()),
                        BigDecimal.valueOf(body.getLon())
                );
            }
        } catch (Exception e) {
            log.error("Geocoding error for '{}': {}", address, e.getMessage());
        }

        return null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CustomGeocodingResponse {
        private double lat;
        private double lon;
        private String address;
        @JsonProperty("source_url")
        private String sourceUrl;
    }
}

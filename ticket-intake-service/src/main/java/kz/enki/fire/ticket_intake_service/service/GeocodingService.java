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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${geocoding.photon.url:https://photon.komoot.io/api/}")
    private String photonUrl;

    public GeocodingResult geocode(Map<String, String> addressComponents) {
        String fullAddress = addressComponents.values().stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));

        if (fullAddress.isBlank()) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(photonUrl)
                    .queryParam("q", fullAddress)
                    .queryParam("limit", 1)
                    .toUriString();

            log.info("Geocoding via Photon: {}", fullAddress);

            ResponseEntity<PhotonResponse> response = restTemplate.getForEntity(url, PhotonResponse.class);
            PhotonResponse body = response.getBody();

            if (body != null && body.getFeatures() != null && !body.getFeatures().isEmpty()) {
                List<Double> coords = body.getFeatures().get(0).getGeometry().getCoordinates();
                // Photon returns [lon, lat]
                log.info("Found coordinates: lon={}, lat={}", coords.get(0), coords.get(1));
                return new GeocodingResult(
                        BigDecimal.valueOf(coords.get(1)), // lat
                        BigDecimal.valueOf(coords.get(0))  // lon
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
    private static class PhotonResponse {
        private List<Feature> features;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Feature {
        private Geometry geometry;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Geometry {
        private List<Double> coordinates;
    }
}

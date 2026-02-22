package kz.enki.fire.ticket_intake_service.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingLookupResponse;
import kz.enki.fire.ticket_intake_service.dto.response.GeocodingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${geocoding.custom.url:http://2.133.130.153:5000/geocode}")
    private String geocodingUrl;

    public GeocodingLookupResponse lookup(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        List<String> candidates = buildAddressCandidates(address);
        if (candidates.isEmpty()) {
            return null;
        }

        for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            GeocodingLookupResponse result = lookupSingleCandidate(address, candidate, i == 0);
            if (result != null) {
                if (i > 0) {
                    log.info("Geocoding succeeded with shortened address '{}' (source='{}')", candidate, address);
                }
                return result;
            }
        }

        return null;
    }

    public GeocodingResult geocode(String address) {
        GeocodingLookupResponse lookupResponse = lookup(address);
        if (lookupResponse == null) return null;
        return new GeocodingResult(lookupResponse.getLatitude(), lookupResponse.getLongitude());
    }

    private GeocodingLookupResponse lookupSingleCandidate(String sourceAddress, String candidateAddress, boolean primary) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(geocodingUrl)
                    .replaceQueryParam("address", candidateAddress)
                    .build()
                    .encode()
                    .toUriString();

            if (primary) {
                log.info("Geocoding lookup via 2GIS bridge: {}", candidateAddress);
            } else {
                log.debug("Geocoding retry with shortened address: {}", candidateAddress);
            }
            ResponseEntity<CustomGeocodingResponse> response = restTemplate.getForEntity(url, CustomGeocodingResponse.class);
            CustomGeocodingResponse body = response.getBody();

            if (body == null) {
                return null;
            }

            return GeocodingLookupResponse.builder()
                    .query(sourceAddress)
                    .latitude(BigDecimal.valueOf(body.getLat()))
                    .longitude(BigDecimal.valueOf(body.getLon()))
                    .resolvedAddress(body.getAddress())
                    .sourceUrl(body.getSourceUrl())
                    .build();
        } catch (Exception e) {
            log.debug("Geocoding candidate failed for '{}': {}", candidateAddress, e.getMessage());
            return null;
        }
    }

    private List<String> buildAddressCandidates(String address) {
        String normalized = normalizeAddress(address);
        if (normalized.isBlank()) {
            return List.of();
        }

        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, normalized);

        List<String> parts = Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        if (parts.size() > 1) {
            for (int size = parts.size() - 1; size >= 1; size--) {
                addCandidate(candidates, String.join(", ", parts.subList(0, size)));
            }
            if (parts.size() >= 3) {
                addCandidate(candidates, parts.get(2));
            }
            addCandidate(candidates, parts.get(parts.size() - 1));
        }

        List<String> words = Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .toList();
        if (words.size() > 3) {
            for (int size = words.size() - 1; size >= 2; size--) {
                addCandidate(candidates, String.join(" ", words.subList(0, size)));
            }
        }

        List<String> result = new ArrayList<>(candidates);
        if (result.size() > 12) {
            return result.subList(0, 12);
        }
        return result;
    }

    private String normalizeAddress(String address) {
        return address
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\u000B', ' ')
                .replace('"', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*,\\s*", ", ")
                .trim();
    }

    private void addCandidate(Set<String> candidates, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return;
        }
        if (normalized.length() < 3 || normalized.matches("^\\d+[\\p{L}\\d-]*$")) {
            return;
        }
        if (normalized.length() > 220) {
            normalized = normalized.substring(0, 220).trim();
        }
        candidates.add(normalized);
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

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

        try {
            String url = UriComponentsBuilder.fromHttpUrl(geocodingUrl)
                    .replaceQueryParam("address", address)
                    .build()
                    .encode()
                    .toUriString();

            log.info("Geocoding lookup via 2GIS bridge: {}", address);

            ResponseEntity<CustomGeocodingResponse> response = restTemplate.getForEntity(url, CustomGeocodingResponse.class);
            CustomGeocodingResponse body = response.getBody();

            if (body != null) {
                return GeocodingLookupResponse.builder()
                        .query(address)
                        .latitude(BigDecimal.valueOf(body.getLat()))
                        .longitude(BigDecimal.valueOf(body.getLon()))
                        .resolvedAddress(body.getAddress())
                        .sourceUrl(body.getSourceUrl())
                        .build();
            }
        } catch (Exception e) {
            log.error("Geocoding lookup error for '{}': {}", address, e.getMessage());
        }

        return null;
    }

    public GeocodingResult geocode(String address) {
        GeocodingLookupResponse lookupResponse = lookup(address);
        if (lookupResponse == null) return null;
        return new GeocodingResult(lookupResponse.getLatitude(), lookupResponse.getLongitude());
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

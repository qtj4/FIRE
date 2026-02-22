package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GeocodingLookupResponse {
    String query;
    BigDecimal latitude;
    BigDecimal longitude;
    String resolvedAddress;
    String sourceUrl;
}

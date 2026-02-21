package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GeocodingResult {
    private final BigDecimal latitude;
    private final BigDecimal longitude;
}

package kz.enki.fire.evaluation_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OfficeResponse {
    private Long id;
    private String code;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

package kz.enki.fire.evaluation_service.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OfficeCreateOrUpdateRequest {
    private String code;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

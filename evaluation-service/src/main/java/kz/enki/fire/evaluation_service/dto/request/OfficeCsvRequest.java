package kz.enki.fire.evaluation_service.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OfficeCsvRequest {
    @CsvBindByName(column = "Офис")
    private String name;

    @CsvBindByName(column = "Адрес")
    private String address;

    @CsvBindByName(column = "Широта")
    private BigDecimal latitude;

    @CsvBindByName(column = "Долгота")
    private BigDecimal longitude;
}

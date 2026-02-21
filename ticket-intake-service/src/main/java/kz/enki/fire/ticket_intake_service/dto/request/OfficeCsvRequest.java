package kz.enki.fire.ticket_intake_service.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class OfficeCsvRequest {
    @CsvBindByName(column = "Офис")
    private String name;

    @CsvBindByName(column = "Адрес")
    private String address;
}

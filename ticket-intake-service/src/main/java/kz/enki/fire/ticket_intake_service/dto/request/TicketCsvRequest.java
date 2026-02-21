package kz.enki.fire.ticket_intake_service.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class TicketCsvRequest {
    @CsvBindByName(column = "GUID клиента")
    private String clientGuid;

    @CsvBindByName(column = "Пол клиента")
    private String clientGender;

    @CsvBindByName(column = "Дата рождения")
    private String birthDate;

    @CsvBindByName(column = "Описание")
    private String description;

    @CsvBindByName(column = "Вложения")
    private String attachments;

    @CsvBindByName(column = "Сегмент клиента")
    private String clientSegment;

    @CsvBindByName(column = "Страна")
    private String country;

    @CsvBindByName(column = "Область")
    private String region;

    @CsvBindByName(column = "Населённый пункт")
    private String city;

    @CsvBindByName(column = "Улица")
    private String street;

    @CsvBindByName(column = "Дом")
    private String houseNumber;
}

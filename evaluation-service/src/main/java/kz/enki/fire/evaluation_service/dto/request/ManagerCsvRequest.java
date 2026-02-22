package kz.enki.fire.evaluation_service.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class ManagerCsvRequest {
    @CsvBindByName(column = "ФИО")
    private String fullName;

    @CsvBindByName(column = "Должность")
    private String position;

    @CsvBindByName(column = "Офис")
    private String officeName;

    @CsvBindByName(column = "Код офиса")
    private String officeCode;

    @CsvBindByName(column = "Навыки")
    private String skills;

    @CsvBindByName(column = "Количество обращений в работе")
    private Integer activeTicketsCount;
}

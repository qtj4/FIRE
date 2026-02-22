package kz.enki.fire.evaluation_service.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class ManagerCsvRequest {
    @CsvBindByName(column = "ФИО")
    private String fullName;

    @CsvBindByName(column = "full_name")
    private String fullNameAltSnake;

    @CsvBindByName(column = "fullName")
    private String fullNameAltCamel;

    @CsvBindByName(column = "name")
    private String fullNameAltName;

    @CsvBindByName(column = "Должность")
    private String position;

    @CsvBindByName(column = "position")
    private String positionAlt;

    @CsvBindByName(column = "Офис")
    private String officeName;

    @CsvBindByName(column = "office_name")
    private String officeNameAltSnake;

    @CsvBindByName(column = "officeName")
    private String officeNameAltCamel;

    @CsvBindByName(column = "office")
    private String officeNameAlt;

    @CsvBindByName(column = "Навыки")
    private String skills;

    @CsvBindByName(column = "skills")
    private String skillsAlt;

    @CsvBindByName(column = "Количество обращений в работе")
    private Integer activeTicketsCount;

    @CsvBindByName(column = "active_tickets_count")
    private Integer activeTicketsCountAltSnake;

    @CsvBindByName(column = "activeTicketsCount")
    private Integer activeTicketsCountAltCamel;

    public String resolveFullName() {
        return firstNonBlank(fullName, fullNameAltSnake, fullNameAltCamel, fullNameAltName);
    }

    public String resolvePosition() {
        return firstNonBlank(position, positionAlt);
    }

    public String resolveOfficeName() {
        return firstNonBlank(officeName, officeNameAltSnake, officeNameAltCamel, officeNameAlt);
    }

    public String resolveSkills() {
        return firstNonBlank(skills, skillsAlt);
    }

    public Integer resolveActiveTicketsCount() {
        return firstNonNull(activeTicketsCount, activeTicketsCountAltSnake, activeTicketsCountAltCamel);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) return null;
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }
}

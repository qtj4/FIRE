package kz.enki.fire.evaluation_service.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OfficeCsvRequest {
    @CsvBindByName(column = "Офис")
    private String name;

    @CsvBindByName(column = "name")
    private String nameAlt;

    @CsvBindByName(column = "office_name")
    private String nameAltSnake;

    @CsvBindByName(column = "office")
    private String nameAltOffice;

    @CsvBindByName(column = "Адрес")
    private String address;

    @CsvBindByName(column = "address")
    private String addressAlt;

    @CsvBindByName(column = "Широта")
    private BigDecimal latitude;

    @CsvBindByName(column = "latitude")
    private BigDecimal latitudeAlt;

    @CsvBindByName(column = "lat")
    private BigDecimal latitudeAltShort;

    @CsvBindByName(column = "Долгота")
    private BigDecimal longitude;

    @CsvBindByName(column = "longitude")
    private BigDecimal longitudeAlt;

    @CsvBindByName(column = "lon")
    private BigDecimal longitudeAltShort;

    public String resolveName() {
        return firstNonBlank(name, nameAlt, nameAltSnake, nameAltOffice);
    }

    public String resolveAddress() {
        return firstNonBlank(address, addressAlt);
    }

    public BigDecimal resolveLatitude() {
        return firstNonNull(latitude, latitudeAlt, latitudeAltShort);
    }

    public BigDecimal resolveLongitude() {
        return firstNonNull(longitude, longitudeAlt, longitudeAltShort);
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

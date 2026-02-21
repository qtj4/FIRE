package kz.enki.fire.ticket_intake_service.mapper;

import kz.enki.fire.ticket_intake_service.dto.request.TicketCsvRequest;
import kz.enki.fire.ticket_intake_service.model.RawTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RawTicketMapper {

    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "clientGuid", source = "clientGuid", qualifiedByName = "stringToUuid")
    @Mapping(target = "birthDate", source = "birthDate", qualifiedByName = "stringToDateTime")
    RawTicket toEntity(TicketCsvRequest request);

    @Named("stringToUuid")
    default UUID stringToUuid(String guid) {
        if (guid == null || guid.isBlank()) return null;
        return UUID.fromString(guid);
    }

    @Named("stringToDateTime")
    default LocalDateTime stringToDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}

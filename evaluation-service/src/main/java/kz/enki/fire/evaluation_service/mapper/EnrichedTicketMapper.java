package kz.enki.fire.evaluation_service.mapper;

import kz.enki.fire.evaluation_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrichedTicketMapper {

    @Mapping(target = "id", source = "enrichedTicketId")
    @Mapping(target = "rawTicket", ignore = true)
    @Mapping(target = "assignedOffice", ignore = true)
    @Mapping(target = "assignedManager", ignore = true)
    EnrichedTicket toEntity(EnrichedTicketEvent event);
}

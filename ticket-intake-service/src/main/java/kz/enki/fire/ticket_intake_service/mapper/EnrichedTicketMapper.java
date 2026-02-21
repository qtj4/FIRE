package kz.enki.fire.ticket_intake_service.mapper;

import kz.enki.fire.ticket_intake_service.dto.kafka.EnrichedTicketEvent;
import kz.enki.fire.ticket_intake_service.model.EnrichedTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrichedTicketMapper {

    @Mapping(target = "enrichedTicketId", source = "id")
    EnrichedTicketEvent toEvent(EnrichedTicket enrichedTicket);
}

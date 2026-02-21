package kz.enki.fire.ticket_intake_service.repository;

import kz.enki.fire.ticket_intake_service.model.EnrichedTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrichedTicketRepository extends JpaRepository<EnrichedTicket, Long> {
}

package kz.enki.fire.ticket_intake_service.repository;

import kz.enki.fire.ticket_intake_service.model.RawTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawTicketRepository extends JpaRepository<RawTicket, Long> {
}

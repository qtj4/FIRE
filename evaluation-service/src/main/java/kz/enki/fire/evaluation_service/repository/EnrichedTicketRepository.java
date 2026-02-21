package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrichedTicketRepository extends JpaRepository<EnrichedTicket, Long> {
}

package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrichedTicketRepository extends JpaRepository<EnrichedTicket, Long> {
    Optional<EnrichedTicket> findByRawTicketId(Long rawTicketId);
}

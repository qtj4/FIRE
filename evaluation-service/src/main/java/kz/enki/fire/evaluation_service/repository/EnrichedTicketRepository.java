package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.EnrichedTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnrichedTicketRepository extends JpaRepository<EnrichedTicket, Long> {
    Optional<EnrichedTicket> findByRawTicketId(Long rawTicketId);

    Optional<EnrichedTicket> findByRawTicketIdAndClientGuid(Long rawTicketId, UUID clientGuid);

    Optional<EnrichedTicket> findByClientGuid(UUID clientGuid);
}

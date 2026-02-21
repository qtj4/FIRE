package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.RawTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RawTicketRepository extends JpaRepository<RawTicket, Long> {

    Optional<RawTicket> findByClientGuid(UUID clientGuid);
}

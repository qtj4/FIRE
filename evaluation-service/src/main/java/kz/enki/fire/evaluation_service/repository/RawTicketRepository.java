package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.RawTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawTicketRepository extends JpaRepository<RawTicket, Long> {
}

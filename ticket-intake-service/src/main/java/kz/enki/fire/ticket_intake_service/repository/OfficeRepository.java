package kz.enki.fire.ticket_intake_service.repository;

import kz.enki.fire.ticket_intake_service.model.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OfficeRepository extends JpaRepository<Office, Long> {
    Optional<Office> findByName(String name);
}

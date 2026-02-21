package kz.enki.fire.ticket_intake_service.repository;

import kz.enki.fire.ticket_intake_service.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
}

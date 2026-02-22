package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.Office;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficeRepository extends JpaRepository<Office, Long> {
    Optional<Office> findByName(String name);
    Optional<Office> findByCode(String code);
}

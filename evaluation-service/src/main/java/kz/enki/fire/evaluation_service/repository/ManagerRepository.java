package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    List<Manager> findByOfficeName(String officeName);
}

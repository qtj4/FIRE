package kz.enki.fire.evaluation_service.repository;

import kz.enki.fire.evaluation_service.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    
    @Query("SELECT m FROM Manager m WHERE m.location = :location AND m.isActive = true ORDER BY m.name")
    List<Manager> findActiveManagersByLocation(@Param("location") String location);
    
    @Query("SELECT m FROM Manager m WHERE m.location = :location AND m.isActive = true AND " +
           "(m.skills LIKE %:skills% OR m.skills = :skills) ORDER BY m.name")
    List<Manager> findActiveManagersByLocationAndSkills(@Param("location") String location, @Param("skills") String skills);
    
    @Query("SELECT m FROM Manager m WHERE m.location = :location AND m.isActive = true AND " +
           "m.skills IS NOT NULL AND m.skills != '' ORDER BY m.name")
    List<Manager> findActiveManagersWithSkillsByLocation(@Param("location") String location);
    
    @Query("SELECT COUNT(m) FROM Manager m WHERE m.location = :location AND m.isActive = true")
    long countActiveManagersByLocation(@Param("location") String location);
}

package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.entity.Manager;
import kz.enki.fire.evaluation_service.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    
    private final ManagerRepository managerRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String MANAGER_LOAD_PREFIX = "manager:load:";
    
    public List<Manager> findCandidates(String location) {
        log.info("Searching for manager candidates in location: {}", location);
        
        List<Manager> candidates = managerRepository.findActiveManagersByLocation(location);
        
        log.info("Found {} manager candidates in location: {}", candidates.size(), location);
        
        return candidates;
    }
    
    public List<Manager> findCandidatesBySkills(String location, String requiredSkills) {
        log.info("Searching for manager candidates in location: {} with skills: {}", location, requiredSkills);
        
        List<Manager> candidates = managerRepository.findActiveManagersByLocationAndSkills(location, requiredSkills);
        
        log.info("Found {} manager candidates with required skills", candidates.size());
        
        return candidates;
    }
    
    public List<Manager> findFallbackCandidates(String location) {
        log.info("Searching for fallback manager candidates in location: {}", location);
        
        List<Manager> candidates = managerRepository.findActiveManagersWithSkillsByLocation(location);
        
        if (candidates.isEmpty()) {
            candidates = managerRepository.findActiveManagersByLocation(location);
        }
        
        log.info("Found {} fallback manager candidates", candidates.size());
        
        return candidates;
    }
    
    public boolean hasActiveManagers(String location) {
        long count = managerRepository.countActiveManagersByLocation(location);
        log.info("Location {} has {} active managers", location, count);
        return count > 0;
    }
    
    public Manager findById(Long id) {
        log.debug("Finding manager by ID: {}", id);
        return managerRepository.findById(id).orElse(null);
    }
    
    public Integer getManagerLoad(Long managerId) {
        try {
            String loadStr = redisTemplate.opsForValue().get(MANAGER_LOAD_PREFIX + managerId);
            return loadStr != null ? Integer.parseInt(loadStr) : 0;
        } catch (Exception e) {
            log.error("Error getting manager load for ID: {}", managerId, e);
            return 0;
        }
    }
    
    public ResponseEntity<List<Manager>> findManagersByLocationSync(String location) {
        try {
            List<Manager> managers = findCandidates(location);
            return ResponseEntity.ok(managers);
        } catch (Exception e) {
            log.error("Error finding managers", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    public ResponseEntity<Manager> findManagerByIdSync(Long id) {
        try {
            Manager manager = findById(id);
            if (manager != null) {
                return ResponseEntity.ok(manager);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting manager", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    public ResponseEntity<String> getManagerLoadSync(Long id) {
        try {
            Integer load = getManagerLoad(id);
            return ResponseEntity.ok("Текущая нагрузка менеджера: " + load);
        } catch (Exception e) {
            log.error("Error getting manager load", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

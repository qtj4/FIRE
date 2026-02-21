package kz.enki.fire.evaluation_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import kz.enki.fire.evaluation_service.dto.AIAnalysisResult;
import kz.enki.fire.evaluation_service.dto.FinalDistribution;
import kz.enki.fire.evaluation_service.dto.TicketContext;
import kz.enki.fire.evaluation_service.entity.Manager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class EvaluationService {
    
    private final AIService aiService;
    private final ManagerService managerService;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    private static final String FINAL_DISTRIBUTION_TOPIC = "final_distribution";
    private static final String MANAGER_LOAD_PREFIX = "manager:load:";
    
    public void processTicket(@Valid TicketContext ticketContext) {
        log.info("Processing ticket: location={}, language={}", ticketContext.getLocation(), ticketContext.getLanguage());
        
        CompletableFuture<AIAnalysisResult> aiAnalysisFuture = CompletableFuture.supplyAsync(
            () -> aiService.analyzeWithFallback(ticketContext.getText())
        );
        
        CompletableFuture<List<Manager>> managersFuture = CompletableFuture.supplyAsync(() -> {
            String location = ticketContext.getLocation();
            if (location == null || location.trim().isEmpty()) {
                try {
                    AIAnalysisResult aiResult = aiAnalysisFuture.get();
                    location = aiResult.getGeoNormalized();
                    if (location == null || location.trim().isEmpty()) {
                        location = "Алматы";
                    }
                } catch (Exception e) {
                    location = "Алматы";
                }
            }
            return managerService.findCandidates(location);
        });
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(aiAnalysisFuture, managersFuture);
        
        allFutures.thenRun(() -> {
            try {
                AIAnalysisResult aiResult = aiAnalysisFuture.get();
                List<Manager> managers = managersFuture.get();
                
                Manager selectedManager = selectBestManager(managers, aiResult.getSentiment(), aiResult.getGeoNormalized());
                
                if (selectedManager != null) {
                    incrementManagerLoad(selectedManager.getId());
                    
                    FinalDistribution distribution = createFinalDistribution(selectedManager, aiResult);
                    sendToFinalDistribution(distribution);
                    
                    recordMetrics(aiResult.getGeoNormalized(), aiResult.getPriority().toString());
                    
                    log.info("Ticket processed successfully. Assigned to manager: {}", selectedManager.getName());
                } else {
                    log.warn("No suitable manager found for ticket with sentiment: {}", aiResult.getSentiment());
                }
                
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error processing ticket", e);
                Thread.currentThread().interrupt();
            }
        }).exceptionally(throwable -> {
            log.error("Error in ticket processing pipeline", throwable);
            return null;
        });
    }
    
    private Manager selectBestManager(List<Manager> managers, String sentiment, String location) {
        if (managers.isEmpty()) {
            return null;
        }
        
        List<Manager> filteredManagers = managers.stream()
            .filter(manager -> hasRequiredSkills(manager, sentiment))
            .toList();
        
        List<Manager> candidates = filteredManagers.isEmpty() ? managers : filteredManagers;
        
        if (filteredManagers.isEmpty()) {
            log.info("No managers with required skills found, using fallback logic");
            List<Manager> fallbackCandidates = managerService.findFallbackCandidates(location);
            if (!fallbackCandidates.isEmpty()) {
                candidates = fallbackCandidates;
            }
        }
        
        return candidates.stream()
            .min((m1, m2) -> {
                int load1 = getManagerLoad(m1.getId());
                int load2 = getManagerLoad(m2.getId());
                return Integer.compare(load1, load2);
            })
            .orElse(candidates.get(0));
    }
    
    private boolean hasRequiredSkills(Manager manager, String category) {
        if (manager.getSkills() == null || category == null) {
            return false;
        }
        return manager.getSkills().contains(category);
    }
    
    private int getManagerLoad(Long managerId) {
        String loadStr = redisTemplate.opsForValue().get(MANAGER_LOAD_PREFIX + managerId);
        return loadStr != null ? Integer.parseInt(loadStr) : 0;
    }
    
    private void incrementManagerLoad(Long managerId) {
        redisTemplate.opsForValue().increment(MANAGER_LOAD_PREFIX + managerId);
        log.debug("Incremented load for manager: {}", managerId);
    }
    
    private FinalDistribution createFinalDistribution(Manager manager, AIAnalysisResult aiResult) {
        FinalDistribution distribution = new FinalDistribution();
        distribution.setManagerId(manager.getId());
        distribution.setManagerName(manager.getName());
        distribution.setSentiment(aiResult.getSentiment());
        distribution.setPriority(aiResult.getPriority());
        distribution.setLanguage(aiResult.getLanguage());
        distribution.setSummary(aiResult.getSummary());
        distribution.setTimestamp(LocalDateTime.now());
        return distribution;
    }
    
    private void sendToFinalDistribution(FinalDistribution distribution) {
        try {
            kafkaTemplate.send(FINAL_DISTRIBUTION_TOPIC, distribution).get();
            log.info("Successfully sent distribution to Kafka: {}", distribution.getManagerId());
        } catch (Exception e) {
            log.error("Failed to send distribution to Kafka", e);
        }
    }
    
    public ResponseEntity<String> processTicketSync(TicketContext ticketContext) {
        try {
            this.processTicket(ticketContext);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Заявка принята в обработку");
        } catch (Exception e) {
            log.error("Error processing ticket via REST API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при обработке заявки: " + e.getMessage());
        }
    }

    public ResponseEntity<AIAnalysisResult> analyzeTextSync(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            AIAnalysisResult result = aiService.analyzeWithFallback(text);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error analyzing text", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<FinalDistribution> simulateDistributionSync(TicketContext ticketContext) {
        try {
            AIAnalysisResult aiResult = aiService.analyzeWithFallback(ticketContext.getText());
            
            String location = ticketContext.getLocation();
            if (location == null || location.trim().isEmpty()) {
                location = aiResult.getGeoNormalized();
                if (location == null || location.trim().isEmpty()) {
                    location = "Алматы";
                }
            }
            
            List<Manager> managers = managerService.findCandidates(location);
            if (managers.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Manager selectedManager = managers.get(0);
            
            FinalDistribution distribution = FinalDistribution.builder()
                    .managerId(selectedManager.getId())
                    .managerName(selectedManager.getName())
                    .sentiment(aiResult.getSentiment())
                    .priority(aiResult.getPriority())
                    .language(aiResult.getLanguage())
                    .summary(aiResult.getSummary())
                    .build();
            
            return ResponseEntity.ok(distribution);
            
        } catch (Exception e) {
            log.error("Error simulating distribution", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private void recordMetrics(String region, String priority) {
        Counter.builder("tickets_distributed_total")
            .tag("region", region.isEmpty() ? "unknown" : region)
            .tag("priority", priority)
            .tag("sentiment", "neutral")
            .register(meterRegistry)
            .increment();
        
        log.debug("Recorded metric for region: {}, priority: {}", region, priority);
    }
}

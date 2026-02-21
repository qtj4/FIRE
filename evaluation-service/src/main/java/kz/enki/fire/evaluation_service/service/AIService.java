package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.AIAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class AIService {
    
    private final Random random = new Random();
    private final List<String> sentiments = Arrays.asList("Позитивный", "Нейтральный", "Негативный");
    private final List<String> languages = Arrays.asList("KZ", "RU", "ENG");
    
    public AIAnalysisResult analyze(String text) {
        log.info("Analyzing text: {}", text);
        
        try {
            Thread.sleep(100 + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AIAnalysisResult result = new AIAnalysisResult();
        result.setSentiment(sentiments.get(random.nextInt(sentiments.size())));
        result.setPriority(random.nextInt(10) + 1);
        result.setLanguage(languages.get(random.nextInt(languages.size())));
        result.setSummary(generateSummary(text));
        result.setGeoNormalized(extractGeoLocation(text));
        
        log.info("AI analysis completed: sentiment={}, priority={}, language={}, summary={}, geo={}", 
                result.getSentiment(), result.getPriority(), result.getLanguage(), 
                result.getSummary(), result.getGeoNormalized());
        
        return result;
    }
    
    public AIAnalysisResult analyzeWithFallback(String text) {
        try {
            return analyze(text);
        } catch (Exception e) {
            log.warn("AI analysis failed, using fallback: {}", e.getMessage());
            AIAnalysisResult result = new AIAnalysisResult();
            result.setSentiment("Нейтральный");
            result.setPriority(5);
            result.setLanguage("RU");
            result.setSummary("Требуется ручной анализ. Обращение требует внимания менеджера.");
            result.setGeoNormalized("");
            return result;
        }
    }
    
    private String generateSummary(String text) {
        if (text.length() <= 50) {
            return text + ". Рекомендуется изучить детали обращения.";
        }
        String truncated = text.substring(0, 47) + "...";
        return truncated + ". Рекомендуется изучить детали обращения.";
    }
    
    private String extractGeoLocation(String text) {
        List<String> cities = Arrays.asList("Алматы", "Астана", "Шымкент", "Атырау", "Актау");
        List<String> countries = Arrays.asList("Казахстан");
        
        for (String city : cities) {
            if (text.toLowerCase().contains(city.toLowerCase())) {
                return "Казахстан, " + city;
            }
        }
        
        for (String country : countries) {
            if (text.toLowerCase().contains(country.toLowerCase())) {
                return country;
            }
        }
        
        return "";
    }
}

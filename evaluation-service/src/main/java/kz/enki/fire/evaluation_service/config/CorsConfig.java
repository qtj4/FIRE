package kz.enki.fire.evaluation_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:8081,http://127.0.0.1:8081,http://2.133.130.153:8081}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Разрешенные origins
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Разрешенные методы
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Разрешенные headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Разрешить credentials
        configuration.setAllowCredentials(true);
        
        // Preflight cache
        configuration.setMaxAge(3600L);
        
        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
            "Content-Type", "Authorization", "X-Requested-With"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

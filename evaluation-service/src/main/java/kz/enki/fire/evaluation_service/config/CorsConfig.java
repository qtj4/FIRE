package kz.enki.fire.evaluation_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.security.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.security.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = split(allowedOrigins);
        List<String> methods = split(allowedMethods);
        List<String> headers = split(allowedHeaders);

        var registration = registry.addMapping("/**")
                .allowedMethods(methods.toArray(String[]::new))
                .allowedHeaders(headers.toArray(String[]::new))
                .allowCredentials(allowCredentials)
                .maxAge(3600);

        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            registration.allowedOriginPatterns("*");
        } else {
            registration.allowedOrigins(origins.toArray(String[]::new));
        }
    }

    private List<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

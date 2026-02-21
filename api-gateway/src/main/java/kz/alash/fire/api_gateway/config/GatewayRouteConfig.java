package kz.alash.fire.api_gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Value("${gateway.use-direct-urls:true}")
    private boolean useDirectUrls;

    @Value("${gateway.services.ticket-intake-service}")
    private String ticketIntakeServiceUrl;

    @Value("${gateway.services.evaluation-service}")
    private String evaluationServiceUrl;

    @Bean
    public DiscoveryLocatorProperties customDiscoveryLocatorProperties(DiscoveryLocatorProperties properties) {
        properties.setEnabled(!useDirectUrls);
        return properties;
    }

    private String getServiceUri(String serviceName) {
        return useDirectUrls ? getDirectUrl(serviceName) : "lb://" + serviceName;
    }

    private String getDirectUrl(String serviceName) {
        return switch (serviceName) {
            case "ticket-intake-service" -> ticketIntakeServiceUrl;
            case "evaluation-service" -> evaluationServiceUrl;

            default -> throw new IllegalArgumentException("Unknown service: " + serviceName);
        };
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Service routes
                .route("ticket-intake-service", r -> r
                        .path("/api/tickets/**")
                        .uri(getServiceUri("ticket-intake-service")))

                .route("ticket-intake-service-intake", r -> r
                        .path("/api/intake/ticket-intake/**")
                        .filters(f -> f.rewritePath("/api/intake/ticket-intake/(?<segment>.*)", "/api/v1/intake/${segment}"))
                        .uri(getServiceUri("ticket-intake-service")))

                .route("evaluation-service", r -> r
                        .path("/api/evaluation/**")
                        .uri(getServiceUri("evaluation-service")))

                .route("evaluation-service-intake", r -> r
                        .path("/api/intake/evaluation/**")
                        .filters(f -> f.rewritePath("/api/intake/evaluation/(?<segment>.*)", "/api/v1/intake/${segment}"))
                        .uri(getServiceUri("evaluation-service")))

                .build();
    }
}

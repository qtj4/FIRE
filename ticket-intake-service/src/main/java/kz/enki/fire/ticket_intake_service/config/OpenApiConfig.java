package kz.enki.fire.ticket_intake_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "FIRE Ticket Intake API",
                version = "v1",
                description = "Endpoints for ingesting CSV data into FIRE pipeline",
                contact = @Contact(name = "FIRE Team")
        ),
        servers = {
                @Server(url = "/", description = "Текущий хост (тот же, с которого открыт Swagger)"),
                @Server(url = "http://localhost:8082", description = "Local")
        }
)
public class OpenApiConfig {
}

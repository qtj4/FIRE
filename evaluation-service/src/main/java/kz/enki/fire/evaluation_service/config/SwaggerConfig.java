package kz.enki.fire.evaluation_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8092}")
    private String serverPort;

    @Bean
    public OpenAPI evaluationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Evaluation Service API")
                        .description("API для интеллектуального распределения заявок по менеджерам")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FIRE Team")
                                .email("support@fire.kz")
                                .url("https://fire.kz"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://2.133.130.153:" + serverPort)
                                .description("Production Server")
                ));
    }
}

package com.predykt.accounting.config;

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
    
    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;
    
    @Bean
    public OpenAPI predyktOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PREDYKT Core Accounting API")
                .description("API REST pour la gestion comptable et financière (Module I - MVP 1.0)")
                .version("1.0.0")
                .contact(new Contact()
                    .name("PREDYKT Team")
                    .email("tech@predykt.com")
                    .url("https://predykt.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://predykt.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080" + contextPath)
                    .description("Serveur de développement"),
                new Server()
                    .url("https://api.predykt.com" + contextPath)
                    .description("Serveur de production")
            ));
    }
}
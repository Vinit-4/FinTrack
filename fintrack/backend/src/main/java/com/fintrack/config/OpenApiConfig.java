package com.fintrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Swagger UI (http://localhost:8080/swagger-ui.html).
 * The security scheme adds the "Authorize" button, so you can paste a JWT
 * once and then call every protected endpoint straight from the browser.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fintrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinTrack API")
                        .version("1.0.0")
                        .description("REST API for the FinTrack expense management system.")
                        .contact(new Contact().name("FinTrack")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token returned by /api/auth/login")));
    }
}

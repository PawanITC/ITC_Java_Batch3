package com.itc.funkart.product.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for Springdoc-OpenAPI.
 * <p>
 * This class centralizes the API documentation logic, providing a rich Swagger UI interface
 * and defining the security requirements for JWT-protected endpoints.
 * </p>
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Link the Security Scheme to the API
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createSecurityScheme()))
                .info(new Info()
                        .title("Funkart Product Service API")
                        .version("1.0.0")
                        .description("""
                                # Funkart Product Service API Documentation
                                
                                ## Overview
                                The Product Service manages all product-related operations including Catalog, Categories, and Shopping Carts.
                                
                                ## Security
                                Protected endpoints require a valid JWT issued by the Auth Service. Use the **Authorize** button below to authenticate.
                                
                                ## Standard Responses
                                - **200/201:** Success
                                - **401:** Unauthorized (Invalid/Missing Token)
                                - **403:** Forbidden (Insufficient Permissions/Role)
                                - **404:** Resource Not Found
                                """)
                        .contact(new Contact()
                                .name("Funkart Development Team")
                                .email("support@funkart.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:9090").description("Local Development"),
                        new Server().url("http://api.funkart.com").description("Production")
                ));
    }

    /**
     * Defines the JWT Bearer Authentication scheme for Swagger UI.
     */
    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token in the format: Bearer <token>");
    }
}
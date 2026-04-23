package com.itc.funkart.product_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for Springdoc-OpenAPI
 * 
 * Configures API documentation for Swagger UI
 * Access at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Funkart Product Service API")
                .version("1.0.0")
                .description("""
                    # Funkart Product Service API Documentation
                    
                    ## Overview
                    The Product Service is a microservice responsible for managing all product-related operations 
                    in the Funkart E-commerce platform. It provides APIs for:
                    - **Product Management:** Create, read, update, delete products
                    - **Category Management:** Organize products into categories
                    - **Product Images:** Manage product images with primary image support
                    - **Shopping Cart:** Add/remove items from cart and manage quantities
                    
                    ## Architecture
                    - **Repository Layer:** Data access with Spring Data JPA for PostgreSQL
                    - **Service Layer:** Business logic and validation
                    - **Controller Layer:** REST endpoints with proper error handling
                    - **Database:** PostgreSQL for relational data, MongoDB for product metadata
                    
                    ## Technologies
                    - Spring Boot 3.3.5
                    - Spring Data JPA & MongoDB
                    - PostgreSQL & MongoDB Databases
                    - JUnit 5 & Mockito Testing
                    - JaCoCo Code Coverage
                    - Kafka for event streaming
                    - Redis for caching
                    - OpenTelemetry for tracing
                    
                    ## Features
                    - ✅ Full CRUD operations for Products, Categories, and Cart
                    - ✅ Product image management with primary image selection
                    - ✅ Shopping cart with quantity management
                    - ✅ Caching for improved performance
                    - ✅ Event-driven architecture with Kafka
                    - ✅ Comprehensive test coverage (Unit, Integration, Performance)
                    - ✅ Real-time distributed tracing with OpenTelemetry
                    
                    ## Getting Started
                    1. Start all services: `docker-compose up -d`
                    2. Run the application: `./gradlew bootRun`
                    3. Access API: http://localhost:9090
                    4. View this documentation: http://localhost:9090/swagger-ui.html
                    
                    ## API Standards
                    - **Responses:** Standard HTTP status codes (200, 201, 400, 404, 500)
                    - **Error Format:** Consistent error response structure
                    - **Pagination:** Supported where applicable
                    - **Validation:** Input validation on all endpoints
                    """)
                .contact(new Contact()
                    .name("Funkart Development Team")
                    .email("support@funkart.com")
                    .url("https://www.funkart.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:9090")
                    .description("Local Development Server"),
                new Server()
                    .url("http://localhost:9090")
                    .description("Development Server"),
                new Server()
                    .url("http://api.funkart.com")
                    .description("Production Server")
            ));
    }
}


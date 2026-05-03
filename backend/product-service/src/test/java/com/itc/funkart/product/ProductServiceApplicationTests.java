package com.itc.funkart.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <h2>ProductServiceApplicationTests</h2>
 * <p>
 * This is a smoke test to ensure that the Spring application context loads successfully.
 * It validates that all configurations, bean definitions, and property bindings
 * are correctly set up for the Product Microservice.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context Validation")
class ProductServiceApplicationTests {

    /**
     * Verifies that the Spring Boot application context initializes without errors.
     * Failure here typically indicates missing environment variables,
     * database connection issues, or bean definition conflicts.
     */
    @Test
    @DisplayName("Should load the application context successfully")
    void contextLoads() {
        // This test will fail if the application context cannot be started.
    }

}
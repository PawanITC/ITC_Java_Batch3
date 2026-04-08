package com.itc.funkart.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Fundamental Smoke Test for the API Gateway.
 * <p>
 * This test ensures that the Spring Application Context can initialize successfully.
 * It verifies that all configurations, bean definitions, and property bindings
 * are valid and that there are no circular dependencies.
 */
@SpringBootTest
class ApiGatewayApplicationTests {

    /**
     * Verifies that the application boots up without errors.
     * <p>
     * If this method completes without throwing an exception, it confirms that
     * the infrastructure (Security, Routing, Filters) is structurally sound.
     */
    @Test
    @DisplayName("Context: Should initialize the Spring Boot application container")
    void contextLoads() {
        // This method is purposefully empty. The test fails if the context
        // cannot be loaded due to configuration errors or missing beans.
    }

}
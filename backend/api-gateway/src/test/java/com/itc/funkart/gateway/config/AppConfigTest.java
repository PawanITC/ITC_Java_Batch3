// =============================================================================
// AppConfig + ServiceRegistry unit tests (separate class, same file package)
// =============================================================================

package com.itc.funkart.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <h2>AppConfig + ServiceRegistry — Unit Tests</h2>
 *
 * <p>Verifies the helper logic on {@link AppConfig} and its inner
 * {@link ServiceRegistry.AppServiceRegistry} implementation without
 * starting a Spring context.
 */
class AppConfigTest {

    /**
     * Builds a fully populated {@link AppConfig} record for testing.
     * Services map includes all three canonical services.
     */
    private AppConfig buildConfig(Map<String, String> services) {
        return new AppConfig(
                "http://localhost:5173",
                new AppConfig.Jwt(
                        "super-secret-key-must-be-at-least-32-bytes!!xyz",
                        3_600_000L,
                        3600,
                        "token",
                        false
                ),
                new AppConfig.Github("id", "secret", "http://callback"),
                services
        );
    }

    // -------------------------------------------------------------------------
    // AppConfig.getServiceUrl
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("AppConfig.getServiceUrl")
    class GetServiceUrlTests {

        @Test
        @DisplayName("Returns the URL for a known service")
        void knownService_returnsUrl() {
            AppConfig config = buildConfig(Map.of("user-service", "http://user-service:8081"));

            assertEquals("http://user-service:8081", config.getServiceUrl("user-service"));
        }

        @Test
        @DisplayName("Throws IllegalStateException for an unknown service name")
        void unknownService_throwsException() {
            AppConfig config = buildConfig(Map.of("user-service", "http://user-service:8081"));

            assertThrows(IllegalStateException.class,
                    () -> config.getServiceUrl("nonexistent-service"));
        }

        @Test
        @DisplayName("Throws IllegalStateException when URL is blank")
        void blankUrl_throwsException() {
            AppConfig config = buildConfig(Map.of("user-service", "  "));

            assertThrows(IllegalStateException.class,
                    () -> config.getServiceUrl("user-service"));
        }
    }

    // -------------------------------------------------------------------------
    // ServiceRegistry.AppServiceRegistry
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ServiceRegistry.AppServiceRegistry")
    class ServiceRegistryTests {

        private ServiceRegistry buildRegistry(Map<String, String> services) {
            return new ServiceRegistry.AppServiceRegistry(buildConfig(services));
        }

        @Test
        @DisplayName("userService() returns the user-service URL")
        void userService_returnsCorrectUrl() {
            ServiceRegistry registry = buildRegistry(Map.of(
                    "user-service", "http://user-service:8081",
                    "payment-service", "http://payment-service:8082",
                    "order-service", "http://order-service:8084"
            ));

            assertEquals("http://user-service:8081", registry.userService());
        }

        @Test
        @DisplayName("paymentService() returns the payment-service URL")
        void paymentService_returnsCorrectUrl() {
            ServiceRegistry registry = buildRegistry(Map.of(
                    "user-service", "http://user-service:8081",
                    "payment-service", "http://payment-service:8082",
                    "order-service", "http://order-service:8084"
            ));

            assertEquals("http://payment-service:8082", registry.paymentService());
        }

        @Test
        @DisplayName("orderService() returns the order-service URL")
        void orderService_returnsCorrectUrl() {
            ServiceRegistry registry = buildRegistry(Map.of(
                    "user-service", "http://user-service:8081",
                    "payment-service", "http://payment-service:8082",
                    "order-service", "http://order-service:8084"
            ));

            assertEquals("http://order-service:8084", registry.orderService());
        }

        @Test
        @DisplayName("Throws when a required service URL is missing from config")
        void missingServiceUrl_throwsException() {
            // Only user-service is present; payment and order are absent
            ServiceRegistry registry = buildRegistry(Map.of(
                    "user-service", "http://user-service:8081"
            ));

            assertThrows(IllegalStateException.class, registry::paymentService);
        }
    }
}
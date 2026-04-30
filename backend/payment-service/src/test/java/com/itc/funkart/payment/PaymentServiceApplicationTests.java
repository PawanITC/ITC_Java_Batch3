package com.itc.funkart.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <h2>PaymentServiceApplicationTests</h2>
 * <p>
 * This is a <b>Context Load</b> test. Its purpose is to verify that the
 * Spring ApplicationContext can be initialized successfully with all
 * bean dependencies and configurations.
 * </p>
 * <p>
 * Uses the {@code @ActiveProfiles("test")} annotation to ensure that
 * <b>application-test.yml</b> is used instead of the production or dev configurations.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceApplicationTests {

    /**
     * Verifies that the Spring context starts without errors.
     * If the EntityManagerFactory or Kafka Producers are misconfigured,
     * this test will fail on startup.
     */
    @Test
    void contextLoads() {
        // No logic needed; the successful startup of the test class itself validates the context.
    }
}
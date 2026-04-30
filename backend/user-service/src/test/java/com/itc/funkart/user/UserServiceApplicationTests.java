package com.itc.funkart.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Fundamental context load test for User Service.
 * Ensures all beans, JPA repositories, and Kafka producers
 * are correctly wired before running the suite.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {
    @Test
    void contextLoads() {
        // If this method executes without throwing an exception,
        // the Spring Context is healthy.
    }
}
package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting user-related domain events to Kafka topics.
 * Enables asynchronous communication with downstream services like Analytics and Audit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a signup event when a new user account is successfully created.
     * * @param event The {@link UserSignupEvent} containing {@code userId}, {@code email}, and {@code name}.
     */
    public void publishUserSignupEvent(UserSignupEvent event) {
        try {
            // Records use accessor methods without the 'get' prefix
            kafkaTemplate.send("user-signup", event.userId().toString(), event);
            log.info("✓ Published user-signup event for user: {}", event.userId());
        } catch (Exception ex) {
            log.error("✗ Failed to publish user-signup event for user {}: {}", event.userId(), ex.getMessage());
        }
    }

    /**
     * Publishes a login event whenever a user authenticates via Email or OAuth.
     * * @param event The {@link UserLoginEvent} containing {@code userId}, {@code email}, and {@code loginMethod}.
     */
    public void publishUserLoginEvent(UserLoginEvent event) {
        try {
            kafkaTemplate.send("user-login", event.userId().toString(), event);
            log.info("✓ Published user-login event for user: {}", event.userId());
        } catch (Exception ex) {
            log.error("✗ Failed to publish user-login event for user {}: {}", event.userId(), ex.getMessage());
        }
    }
}
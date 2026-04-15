package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import com.itc.funkart.user.exceptions.InvalidEventException;
import com.itc.funkart.user.exceptions.MessagingException;
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
     * * @param event The {@link UserSignupEvent} containing id, email, name, and role.
     */
    public void publishUserSignupEvent(UserSignupEvent event) {
        // 1. FAIL-FAST VALIDATION (Outside the try-catch)
        // This will throw the exception directly to the caller/test
        if (event.userId() == null) {
            throw new InvalidEventException("User ID is required for signup events");
        }
        try {
            // 2. INFRASTRUCTURE LOGIC
            // We use .get() here to make it synchronous.
            // This forces the code to wait for the "Banker" (Acks=all) confirmation.
            kafkaTemplate.send("user-signup", event.userId().toString(), event).get();
            log.info("✓ Confirmed: user-signup event persisted for user: {}", event.userId());

        } catch (Exception ex) {
            // 3. ONLY INFRASTRUCTURE ERRORS GET WRAPPED
            log.error("✗ Reliable delivery failed for user {}: {}", event.userId(), ex.getMessage());
            throw new MessagingException("We couldn't broadcast your signup. Please try again.", ex);
        }
    }
    /**
     * Publishes a login event whenever a user authenticates via Email or OAuth.
     * * @param event The {@link UserLoginEvent} containing id, email, loginMethod, and role.
     */
    public void publishUserLoginEvent(UserLoginEvent event) {
        // No .get() here = Fast performance for the user
        kafkaTemplate.send("user-login", event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✓ Login event confirmed at offset {}", result.getRecordMetadata().offset());
                    } else {
                        // We log the error for our dev team, but we DON'T throw MessagingException
                        // because we don't want to crash the user's login session.
                        log.error("✗ Background failure: Login event lost for user {}", event.userId(), ex);
                    }
                });
    }
}
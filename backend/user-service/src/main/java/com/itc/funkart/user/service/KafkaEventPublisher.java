package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.InvalidEventException;
import com.itc.funkart.user.exceptions.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

/**
 * Service responsible for broadcasting user-related domain events to Kafka topics.
 * <p>
 * This service acts as an abstraction layer between domain logic and messaging infrastructure,
 * converting {@link User} entities into specialized event DTOs and ensuring appropriate
 * delivery guarantees (Synchronous for Signups, Asynchronous for Logins).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Orchestrates the broadcasting of a User Signup.
     * <p>
     * Converts the domain entity to a {@link UserSignupEvent} and performs a
     * synchronous send to ensure downstream consistency for new accounts.
     * </p>
     *
     * @param user The newly created {@link User} entity.
     * @throws MessagingException if the infrastructure fails to acknowledge the message.
     */
    public void publishSignup(User user) {
        UserSignupEvent event = UserSignupEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .timestamp(System.currentTimeMillis())
                .build();

        sendSignupSync(event);
    }

    /**
     * Orchestrates the broadcasting of a User Login.
     * <p>
     * Converts the domain entity to a {@link UserLoginEvent} and performs an
     * asynchronous send to prioritize user response time over delivery confirmation.
     * </p>
     *
     * @param user   The authenticated {@link User} entity.
     * @param method The authentication method used (e.g., "email", "github").
     */
    public void publishLogin(User user, String method) {
        UserLoginEvent event = UserLoginEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .loginMethod(method)
                .role(user.getRole().name())
                .timestamp(System.currentTimeMillis())
                .build();

        sendLoginAsync(event);
    }

    /**
     * Internal helper to perform a synchronous Kafka send for Signups.
     * Uses {@code .get()} to block until the broker acknowledges receipt.
     *
     * @param event The populated signup event DTO.
     */
    private void sendSignupSync(UserSignupEvent event) {
        if (event.userId() == null) {
            throw new InvalidEventException("User ID is required for signup events");
        }

        try {
            // Synchronous wait for confirmation from Kafka brokers
            kafkaTemplate.send("user-signup", event.userId().toString(), event).get();
            log.info("✓ Confirmed: user-signup event persisted for user: {}", event.userId());

        } catch (InterruptedException | ExecutionException ex) {
            log.error("✗ Reliable delivery failed for user {}: {}", event.userId(), ex.getMessage());
            Thread.currentThread().interrupt(); // Preserve interrupt status
            throw new MessagingException("Critical failure broadcasting signup event.", ex);
        } catch (Exception ex) {
            log.error("✗ Unexpected error during signup broadcast: {}", ex.getMessage());
            throw new MessagingException("Internal error in messaging subsystem.", ex);
        }
    }

    /**
     * Internal helper to perform an asynchronous Kafka send for Logins.
     * Uses a callback mechanism to handle results without blocking the calling thread.
     *
     * @param event The populated login event DTO.
     */
    private void sendLoginAsync(UserLoginEvent event) {
        kafkaTemplate.send("user-login", event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✓ Login event confirmed at offset {}", result.getRecordMetadata().offset());
                    } else {
                        // Background failure: Logged for ops but does not interrupt the user session
                        log.error("✗ Background failure: Login event lost for user {}: {}",
                                event.userId(), ex.getMessage());
                    }
                });
    }
}
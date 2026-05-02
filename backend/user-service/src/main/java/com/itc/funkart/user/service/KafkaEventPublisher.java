package com.itc.funkart.user.service;

import com.itc.funkart.common.constants.messaging.KafkaMessaging;
import com.itc.funkart.common.dto.event.UserLoginEvent;
import com.itc.funkart.common.dto.event.UserSignupEvent;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.InvalidEventException;
import com.itc.funkart.user.exceptions.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

/**
 * <h2>Kafka Event Publisher</h2>
 *
 * <p>Broadcasts user domain events using a "Reliability vs. Speed" strategy.
 * This service leverages the shared contracts library for type safety across the microservices mesh.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a User Signup event.
     * <p><b>Strategy:</b> Synchronous. We block until Kafka acknowledges receipt to
     * ensure downstream services (like Email) can reliably process the new user.</p>
     */
    public void publishSignup(User user) {
        // Using the static factory from common-contracts
        UserSignupEvent event = UserSignupEvent.of(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );

        sendSignupSync(event);
    }

    /**
     * Publishes a User Login event.
     * <p><b>Strategy:</b> Asynchronous. We don't block the auth flow for a login event;
     * if the message fails, we log it for audit but prioritize user response time.</p>
     */
    public void publishLogin(User user, String method) {
        // Using the static factory from common-contracts
        UserLoginEvent event = UserLoginEvent.of(
                user.getId(),
                user.getEmail(),
                method,
                user.getRole().name()
        );

        sendLoginAsync(event);
    }

    /**
     * Reliable delivery for critical signup events.
     */
    private void sendSignupSync(UserSignupEvent event) {
        if (event.userId() == null) {
            throw new InvalidEventException("User ID is required for signup events");
        }

        try {
            // Uses KafkaMessaging.TOPIC_USER_SIGNUP constant from common-contracts
            kafkaTemplate.send(KafkaMessaging.TOPIC_USER_SIGNUP, event.userId().toString(), event).get();
            log.info("✓ Sync success: Topic [{}] for user: {}", KafkaMessaging.TOPIC_USER_SIGNUP, event.userId());

        } catch (InterruptedException | ExecutionException ex) {
            log.error("✗ Sync failure: User {}: {}", event.userId(), ex.getMessage());
            Thread.currentThread().interrupt();
            throw new MessagingException("Critical failure broadcasting signup event.", ex);
        } catch (Exception ex) {
            log.error("✗ Unexpected messaging error: {}", ex.getMessage());
            throw new MessagingException("Internal error in messaging subsystem.", ex);
        }
    }

    /**
     * Non-blocking delivery for login metrics/auditing.
     */
    private void sendLoginAsync(UserLoginEvent event) {
        // Uses KafkaMessaging.TOPIC_AUTH_LOGIN constant from common-contracts
        kafkaTemplate.send(KafkaMessaging.TOPIC_AUTH_LOGIN, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✓ Async success: Topic [{}] offset {}",
                                KafkaMessaging.TOPIC_AUTH_LOGIN, result.getRecordMetadata().offset());
                    } else {
                        log.error("✗ Async failure: Login event lost for user {}: {}",
                                event.userId(), ex.getMessage());
                    }
                });
    }
}
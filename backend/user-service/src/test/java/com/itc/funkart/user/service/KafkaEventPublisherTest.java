package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import com.itc.funkart.user.exceptions.InvalidEventException;
import com.itc.funkart.user.exceptions.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaEventPublisher}.
 * Verifies that domain events are correctly routed to Kafka topics and that
 * synchronous vs. asynchronous error handling behaves according to architectural standards.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "tester@funkart.com";

    // This is the single source of truth for the valid event
    private UserSignupEvent sharedValidEvent;

    @BeforeEach
    void globalSetUp() {
        reset(kafkaTemplate);

        sharedValidEvent = UserSignupEvent.builder()
                .userId(TEST_USER_ID)
                .email(TEST_EMAIL)
                .build();
    }

    /**
     * Test suite for User Signup events.
     * Signup events are critical and must be handled synchronously (Strict).
     */
    @Nested
    @DisplayName("User Signup Events (Strict/Sync)")
    class SignupEvents {

        /**
         * SUCCESS: Verifies that the publisher waits for the broker acknowledgment
         * before completing the method.
         */
        @Test
        @DisplayName("Should successfully publish and acknowledge signup event")
        void publishUserSignupEvent_Success() {
            // Arrange
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // Act
            kafkaEventPublisher.publishUserSignupEvent(sharedValidEvent);

            // Assert: event.userId().toString() results in "1"
            verify(kafkaTemplate).send(eq("user-signup"), eq("1"), eq(sharedValidEvent));
        }


        /**
         * FAILURE: Verifies that a Kafka failure results in a {@link MessagingException},
         * triggering the GlobalExceptionHandler.
         */
        @Test
        @DisplayName("Should throw MessagingException when Kafka broker is down")
        void publishUserSignupEvent_InfrastructureFailure() {
            // Arrange
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Broker Offline"));

            // Use broad matchers to ensure we don't return null and cause NPE
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // Act & Assert
            assertThrows(MessagingException.class, () ->
                    kafkaEventPublisher.publishUserSignupEvent(sharedValidEvent)
            );
        }

        @Test
        @DisplayName("Should throw InvalidEventException when userId is missing")
        void publishUserSignupEvent_ValidationFailure() {
            // Arrange
            UserSignupEvent invalidEvent = UserSignupEvent.builder()
                    .userId(null)
                    .email(TEST_EMAIL)
                    .build();

            // Act & Assert
            assertThrows(InvalidEventException.class, () ->
                    kafkaEventPublisher.publishUserSignupEvent(invalidEvent)
            );

            // Verify send was NEVER called because of fail-fast validation
            verifyNoInteractions(kafkaTemplate);
        }
    }

    /**
     * Test suite for User Login events.
     * Login events are informational and are handled asynchronously (Graceful).
     */
    @Nested
    @DisplayName("User Login Events (Graceful/Async)")
    class LoginEvents {

        /**
         * SUCCESS: Verifies that login events are sent to the correct topic.
         */
        @Test
        @DisplayName("Should publish login event via fire-and-forget mechanism")
        void publishUserLoginEvent_Success() {
            // Arrange
            UserLoginEvent loginEvent = UserLoginEvent.builder()
                    .userId(TEST_USER_ID)
                    .loginMethod("EMAIL")
                    .build();

            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // Act
            kafkaEventPublisher.publishUserLoginEvent(loginEvent);

            // Assert
            verify(kafkaTemplate, times(1))
                    .send(eq("user-login"), eq("1"), eq(loginEvent));
        }
    }
}
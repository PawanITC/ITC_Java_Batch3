package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaEventPublisher}.
 * <p>
 * This suite verifies that domain events are correctly routed to the appropriate
 * Kafka topics and that internal exceptions are caught and logged rather than
 * disrupting the calling service's flow.
 * </p>
 *
 * @author Gemini
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "tester@funkart.com";

    @Nested
    @DisplayName("User Signup Events")
    class SignupEvents {

        @Test
        @DisplayName("SUCCESS: Should publish signup event with correct topic and key")
        void publishUserSignupEvent_Success() {
            // Arrange
            UserSignupEvent event = UserSignupEvent.builder()
                    .userId(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .name("Tester")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Act
            kafkaEventPublisher.publishUserSignupEvent(event);

            // Assert
            // Verify topic is 'user-signup' and key is the stringified user ID
            verify(kafkaTemplate, times(1))
                    .send(eq("user-signup"), eq(TEST_USER_ID.toString()), eq(event));
        }

        @Test
        @DisplayName("FAILURE: Should catch and log exceptions during signup publishing")
        void publishUserSignupEvent_Failure_HandlesException() {
            // Arrange
            UserSignupEvent event = UserSignupEvent.builder()
                    .userId(TEST_USER_ID)
                    .build();

            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Broker Connection Lost"));

            // Act & Assert
            // Ensure no exception bubbles up to the caller
            kafkaEventPublisher.publishUserSignupEvent(event);

            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("User Login Events")
    class LoginEvents {

        @Test
        @DisplayName("SUCCESS: Should publish login event with correct topic and key")
        void publishUserLoginEvent_Success() {
            // Arrange
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .loginMethod("EMAIL")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Act
            kafkaEventPublisher.publishUserLoginEvent(event);

            // Assert
            // Verify topic is 'user-login' and key is the stringified user ID
            verify(kafkaTemplate, times(1))
                    .send(eq("user-login"), eq(TEST_USER_ID.toString()), eq(event));
        }

        @Test
        @DisplayName("FAILURE: Should catch and log exceptions during login publishing")
        void publishUserLoginEvent_Failure_HandlesException() {
            // Arrange
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(TEST_USER_ID)
                    .build();

            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Partition Unreachable"));

            // Act & Assert
            kafkaEventPublisher.publishUserLoginEvent(event);

            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }
    }
}
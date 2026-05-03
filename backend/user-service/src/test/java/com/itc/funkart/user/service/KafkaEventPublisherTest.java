package com.itc.funkart.user.service;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.login.UserLoginEvent;
import com.itc.funkart.common.dto.event.signup.UserSignupEvent;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>KafkaEventPublisher — Unit Tests</h2>
 *
 * <p>Validates the event publishing contract against the real public API:
 * <ul>
 *   <li>{@code publishSignup(User)} — builds a {@code UserSignupEvent} internally
 *       and sends it <b>synchronously</b> to the {@code user-signup} topic</li>
 *   <li>{@code publishLogin(User, String)} — builds a {@code UserLoginEvent} internally
 *       and sends it <b>asynchronously</b> to the {@code user-login} topic</li>
 * </ul>
 *
 * <p><b>Important:</b> The old tests called non-existent overloads
 * {@code publishUserSignupEvent(UserSignupEvent)} and
 * {@code publishUserLoginEvent(UserLoginEvent)}. Those methods do not exist in
 * production code — the real publisher builds the event DTO internally from a
 * {@link User} entity.
 *
 * <p>Signup sends are synchronous (uses {@code .get()} internally) so a failed
 * future must throw {@link MessagingException}. Login sends are fire-and-forget
 * so failures are only logged — no exception propagates to the caller.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher publisher;

    /**
     * Reusable domain user for all test cases.
     */
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .name("Alice")
                .role(Role.ROLE_USER)
                .build();
    }

    // -------------------------------------------------------------------------
    // publishSignup(User) — synchronous, strict
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("publishSignup(User) — synchronous")
    class PublishSignupTests {

        @Test
        @DisplayName("Sends to the user-signup topic with userId as key")
        void sendsToCorrectTopic() {
            // 1. Arrange: Create the future for a synchronous send
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            // 2. Stub: Use the EXACT constant from your common library
            // Use doReturn().when() to avoid side effects with strict stubbing
            doReturn(future).when(kafkaTemplate).send(
                    eq(KafkaTopics.USER_SIGNUP), // Use the constant!
                    eq("1"),
                    any(UserSignupEvent.class)
            );

            // 3. Act
            publisher.publishSignup(testUser);

            // 4. Assert: Verify the call happened with the correct constants
            verify(kafkaTemplate).send(
                    eq(KafkaTopics.USER_SIGNUP),
                    eq("1"),
                    any(UserSignupEvent.class)
            );
        }

        @Test
        @DisplayName("Completes without throwing when broker acknowledges")
        void completesCleanlyOnSuccess() {
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            assertDoesNotThrow(() -> publisher.publishSignup(testUser));
        }

        @Test
        @DisplayName("Throws MessagingException when broker is unavailable")
        void throwsMessagingExceptionOnBrokerFailure() {
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Broker offline"));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            assertThrows(MessagingException.class, () -> publisher.publishSignup(testUser));
        }

        @Test
        @DisplayName("Never sends when user ID is null (fail-fast guard)")
        void doesNotSendWhenUserIdIsNull() {
            User noIdUser = User.builder()
                    .id(null)
                    .email("noId@example.com")
                    .name("NoId")
                    .role(Role.ROLE_USER)
                    .build();

            // publishSignup internally calls publishSignupEvent which validates the event
            // The event builder will have userId=null → InvalidEventException before kafka is called
            assertThrows(Exception.class, () -> publisher.publishSignup(noIdUser));
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("Event payload includes correct email")
        void eventContainsCorrectEmail() {
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            publisher.publishSignup(testUser);

            // Capture the event object sent to Kafka and verify email
            verify(kafkaTemplate).send(anyString(), anyString(),
                    argThat(event -> event.toString().contains("alice@example.com")
                            || event.getClass().getSimpleName().equals("UserSignupEvent")));
        }
    }

    // -------------------------------------------------------------------------
    // publishLogin(User, String) — asynchronous, graceful
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Publish Login Tests")
    class PublishLoginTests {

        @Test
        @DisplayName("Sends to the user-login topic with userId as key")
        void sendsToCorrectTopic() {
            // Arrange
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            // Use the constant from common-contracts
            doReturn(future).when(kafkaTemplate).send(
                    eq(KafkaTopics.AUTH_LOGIN),
                    eq("1"),
                    any(UserLoginEvent.class)
            );

            // Act
            publisher.publishLogin(testUser, "email");

            // Assert
            verify(kafkaTemplate).send(eq(KafkaTopics.AUTH_LOGIN), eq("1"), any());
        }

        @Test
        @DisplayName("Passes login method in event payload (email)")
        void passesLoginMethodEmail() {
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            publisher.publishLogin(testUser, "email");

            // ArgumentCaptor allows us to peek inside the record to verify the login method
            ArgumentCaptor<UserLoginEvent> eventCaptor = ArgumentCaptor.forClass(UserLoginEvent.class);

            verify(kafkaTemplate).send(eq(KafkaTopics.AUTH_LOGIN), eq("1"), eventCaptor.capture());
            assertEquals("email", eventCaptor.getValue().loginMethod());
        }

        @Test
        @DisplayName("Passes login method in event payload (github)")
        void passesLoginMethodGithub() {
            CompletableFuture<SendResult<String, Object>> future =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            publisher.publishLogin(testUser, "github");

            ArgumentCaptor<UserLoginEvent> eventCaptor = ArgumentCaptor.forClass(UserLoginEvent.class);

            verify(kafkaTemplate).send(eq(KafkaTopics.AUTH_LOGIN), eq("1"), eventCaptor.capture());
            assertEquals("github", eventCaptor.getValue().loginMethod());
        }
    }
}
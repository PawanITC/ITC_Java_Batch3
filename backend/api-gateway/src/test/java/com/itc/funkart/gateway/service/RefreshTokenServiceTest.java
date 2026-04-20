package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>RefreshTokenService — Unit Tests</h2>
 *
 * <p>Validates the refresh token lifecycle managed in Redis:
 * <ul>
 *   <li>{@code store} — persists {@code userId:deviceId} under {@code refresh:<token>}</li>
 *   <li>{@code validateAndConsume} — verifies device match, detects replay attacks,
 *       rotates the token (delete old, mark as used)</li>
 *   <li>{@code revoke} — removes the refresh key immediately</li>
 * </ul>
 *
 * <p>Security invariants tested:
 * <ul>
 *   <li>Unknown token → {@link JwtAuthenticationException}</li>
 *   <li>Device mismatch → {@link JwtAuthenticationException}</li>
 *   <li>Reuse of a consumed token → {@link JwtAuthenticationException} (replay detection)</li>
 *   <li>Corrupted value format → {@link JwtAuthenticationException}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private ReactiveRedisTemplate<String, String> redis;
    @Mock private ReactiveValueOperations<String, String> valueOps;

    private RefreshTokenService service;

    /** Token and device constants reused across test cases. */
    private static final String TOKEN   = "refresh-uuid-abc";
    private static final String DEVICE  = "device-xyz";
    private static final Long   USER_ID = 42L;

    /** Canonical Redis key computed the same way the service does. */
    private static final String KEY      = "refresh:" + TOKEN;
    private static final String USED_KEY = "used:" + KEY;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new RefreshTokenService(redis);
    }

    // -------------------------------------------------------------------------
    // store
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("store")
    class StoreTests {

        @Test
        @DisplayName("Persists userId:deviceId under the refresh: key prefix")
        void store_writesCorrectKeyAndValue() {
            Duration ttl = Duration.ofDays(7);
            when(valueOps.set(eq(KEY), eq(USER_ID + ":" + DEVICE), eq(ttl)))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(service.store(TOKEN, USER_ID, DEVICE, ttl))
                    .verifyComplete();

            verify(valueOps).set(KEY, USER_ID + ":" + DEVICE, ttl);
        }

        @Test
        @DisplayName("Mono completes without error on successful write")
        void store_completesCleanly() {
            when(valueOps.set(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(service.store(TOKEN, USER_ID, DEVICE, Duration.ofDays(1)))
                    .verifyComplete();
        }
    }

    // -------------------------------------------------------------------------
    // validateAndConsume — happy path
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateAndConsume — happy path")
    class ValidateAndConsumeHappyPathTests {

        @BeforeEach
        void setUpHappyPath() {
            // Token exists in Redis
            when(valueOps.get(KEY)).thenReturn(Mono.just(USER_ID + ":" + DEVICE));
            // Not previously used
            when(redis.hasKey(USED_KEY)).thenReturn(Mono.just(false));
            // Mark as used
            when(valueOps.set(eq(USED_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));
            // Delete original key
            when(redis.delete(KEY)).thenReturn(Mono.just(1L));
        }

        @Test
        @DisplayName("Returns the correct userId on success")
        void returnsUserId() {
            StepVerifier.create(service.validateAndConsume(TOKEN, DEVICE))
                    .expectNext(USER_ID)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Deletes the original refresh key (token rotation)")
        void deletesOriginalKey() {
            service.validateAndConsume(TOKEN, DEVICE).block();

            verify(redis).delete(KEY);
        }

        @Test
        @DisplayName("Marks the token as used to prevent replay")
        void marksTokenAsUsed() {
            service.validateAndConsume(TOKEN, DEVICE).block();

            verify(valueOps).set(eq(USED_KEY), eq("1"), any(Duration.class));
        }
    }

    // -------------------------------------------------------------------------
    // validateAndConsume — failure cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateAndConsume — failure cases")
    class ValidateAndConsumeFailureTests {

        @Test
        @DisplayName("Throws JwtAuthenticationException when token is unknown")
        void unknownToken_throwsException() {
            when(valueOps.get(KEY)).thenReturn(Mono.empty());

            StepVerifier.create(service.validateAndConsume(TOKEN, DEVICE))
                    .expectError(JwtAuthenticationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException when device ID does not match stored value")
        void deviceMismatch_throwsException() {
            when(valueOps.get(KEY)).thenReturn(Mono.just(USER_ID + ":other-device"));
            when(redis.hasKey(USED_KEY)).thenReturn(Mono.just(false));

            StepVerifier.create(service.validateAndConsume(TOKEN, DEVICE))
                    .expectError(JwtAuthenticationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException on replay (token already consumed)")
        void replayDetected_throwsException() {
            when(valueOps.get(KEY)).thenReturn(Mono.just(USER_ID + ":" + DEVICE));
            when(redis.hasKey(USED_KEY)).thenReturn(Mono.just(true));

            StepVerifier.create(service.validateAndConsume(TOKEN, DEVICE))
                    .expectError(JwtAuthenticationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException when stored value is corrupted (wrong format)")
        void corruptedValue_throwsException() {
            // Value missing the colon separator
            when(valueOps.get(KEY)).thenReturn(Mono.just("corruptedvalue"));
            when(redis.hasKey(USED_KEY)).thenReturn(Mono.just(false));

            StepVerifier.create(service.validateAndConsume(TOKEN, DEVICE))
                    .expectError(JwtAuthenticationException.class)
                    .verify();
        }
    }

    // -------------------------------------------------------------------------
    // revoke
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("revoke")
    class RevokeTests {

        @Test
        @DisplayName("Deletes the refresh key from Redis")
        void deletesKey() {
            when(redis.delete(KEY)).thenReturn(Mono.just(1L));

            StepVerifier.create(service.revoke(TOKEN))
                    .verifyComplete();

            verify(redis).delete(KEY);
        }

        @Test
        @DisplayName("Completes cleanly even if the key did not exist")
        void completesCleanly_whenKeyAbsent() {
            when(redis.delete(KEY)).thenReturn(Mono.just(0L));

            StepVerifier.create(service.revoke(TOKEN))
                    .verifyComplete();
        }
    }
}
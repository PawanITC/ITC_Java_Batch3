package com.itc.funkart.gateway.service;

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
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>TokenBlacklistService — Unit Tests</h2>
 *
 * <p>Validates the token revocation contract:
 * <ul>
 *   <li>{@code isBlacklisted} — true when the Redis key exists, false otherwise</li>
 *   <li>{@code blacklist} — writes a key with the supplied TTL</li>
 *   <li>{@code blacklistWithExpiry} — derives TTL from the JWT's own expiry date</li>
 * </ul>
 *
 * <p>All Redis interactions are mocked; no embedded Redis is required.
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redis;
    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new TokenBlacklistService(redis);
    }

    // -------------------------------------------------------------------------
    // isBlacklisted
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklistedTests {

        @Test
        @DisplayName("Returns true when the key exists in Redis")
        void keyExists_returnsTrue() {
            when(redis.hasKey("blacklist:tok")).thenReturn(Mono.just(true));

            StepVerifier.create(service.isBlacklisted("tok"))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Returns false when the key does not exist in Redis")
        void keyAbsent_returnsFalse() {
            when(redis.hasKey("blacklist:tok")).thenReturn(Mono.just(false));

            StepVerifier.create(service.isBlacklisted("tok"))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Returns false (default) when Redis emits empty")
        void emptyRedis_defaultsFalse() {
            when(redis.hasKey(anyString())).thenReturn(Mono.empty());

            StepVerifier.create(service.isBlacklisted("tok"))
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    // -------------------------------------------------------------------------
    // blacklist
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("blacklist")
    class BlacklistTests {

        @Test
        @DisplayName("Writes 'revoked' to Redis under the blacklist: prefix")
        void writesRevoked_withCorrectKey() {
            when(valueOps.set(eq("blacklist:my-token"), eq("revoked"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(service.blacklist("my-token", Duration.ofMinutes(10)))
                    .verifyComplete();

            verify(valueOps).set("blacklist:my-token", "revoked", Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("Uses the supplied TTL exactly")
        void usesSuppliedTtl() {
            Duration ttl = Duration.ofHours(2);
            when(valueOps.set(anyString(), anyString(), eq(ttl))).thenReturn(Mono.just(true));

            StepVerifier.create(service.blacklist("token", ttl))
                    .verifyComplete();

            verify(valueOps).set(anyString(), anyString(), eq(ttl));
        }
    }

    // -------------------------------------------------------------------------
    // blacklistWithExpiry
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("blacklistWithExpiry")
    class BlacklistWithExpiryTests {

        @Test
        @DisplayName("Writes to Redis when expiry is in the future")
        void futureExpiry_writesToRedis() {
            Date futureExpiry = new Date(System.currentTimeMillis() + 60_000);
            when(valueOps.set(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(service.blacklistWithExpiry("tok", futureExpiry))
                    .verifyComplete();

            verify(valueOps).set(eq("blacklist:tok"), eq("revoked"), any(Duration.class));
        }

        @Test
        @DisplayName("Does nothing (Mono.empty) when expiry is already in the past")
        void pastExpiry_doesNotWriteToRedis() {
            Date pastExpiry = new Date(System.currentTimeMillis() - 10_000);

            StepVerifier.create(service.blacklistWithExpiry("tok", pastExpiry))
                    .verifyComplete();

            verifyNoInteractions(valueOps);
        }
    }
}
package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";

    private final ReactiveRedisTemplate<String, String> redis;

    public RefreshTokenService(ReactiveRedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public Mono<Void> store(String refreshToken, Long userId, String deviceId, Duration ttl) {

        String value = userId + ":" + deviceId;

        return redis.opsForValue()
                .set(PREFIX + refreshToken, value, ttl)
                .then();
    }

    public Mono<Long> validateAndConsume(String refreshToken, String deviceId) {

        String key = PREFIX + refreshToken;
        String usedKey = "used:" + key;

        return redis.opsForValue().get(key)
                .switchIfEmpty(Mono.error(new JwtAuthenticationException("Invalid refresh token")))
                .flatMap(value -> {

                    String[] parts = value.split(":");

                    if (parts.length != 2) {
                        return Mono.error(new JwtAuthenticationException("Corrupted refresh token data"));
                    }

                    Long userId = Long.valueOf(parts[0]);
                    String storedDevice = parts[1];

                    if (!storedDevice.equals(deviceId)) {
                        return Mono.error(new JwtAuthenticationException("Device mismatch"));
                    }

                    return redis.hasKey(usedKey)
                            .flatMap(alreadyUsed -> {

                                if (Boolean.TRUE.equals(alreadyUsed)) {
                                    return Mono.error(new JwtAuthenticationException("Refresh token reuse detected"));
                                }

                                return redis.opsForValue()
                                        .set(usedKey, "1", Duration.ofMinutes(5))
                                        .then(redis.delete(key))
                                        .thenReturn(userId);
                            });
                });
    }

    public Mono<Void> revoke(String refreshToken) {
        return redis.delete(PREFIX + refreshToken).then();
    }
}

package com.itc.funkart.gateway.service;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

@Service
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isBlacklisted(String token) {
        return redisTemplate.hasKey(PREFIX + token)
                .defaultIfEmpty(false);
    }

    public Mono<Void> blacklist(String token, Duration ttl) {
        return redisTemplate.opsForValue()
                .set(PREFIX + token, "revoked", ttl)
                .then();
    }

    public Mono<Void> blacklistWithExpiry(String token, Date expiry) {

        long ttlMillis = expiry.getTime() - System.currentTimeMillis();

        if (ttlMillis <= 0) {
            return Mono.empty();
        }

        return blacklist(token, Duration.ofMillis(ttlMillis));
    }
}
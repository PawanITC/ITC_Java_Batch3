package com.itc.funkart.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * <h2>RedisConfig</h2>
 * <p>
 * Configures the Redis-based caching layer for the Order Service.
 * This configuration enables the use of {@code @Cacheable}, {@code @CachePut},
 * and {@code @CacheEvict} annotations across the service layer.
 * </p>
 *
 * <h3>Key Performance Features:</h3>
 * <ul>
 *   <li><b>JSON Serialization:</b> Uses {@link GenericJackson2JsonRedisSerializer}
 *   to store data as JSON, making the cache readable by non-Java services and
 *   avoiding Java Serialization vulnerabilities.</li>
 *   <li><b>Automatic Expiration:</b> Implements a 10-minute default TTL to ensure
 *   memory efficiency in the Redis cluster and data freshness.</li>
 *   <li><b>JVM Resource Management:</b> Decouples the application's memory (Heap)
 *   from cached data, allowing the service to scale horizontally without
 *   increasing the local memory footprint.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * <h3>Cache Manager Setup</h3>
     * <p>
     * Orchestrates the lifecycle of cached objects. It defines how Java objects
     * are serialized into the Redis store and how long they persist.
     * </p>
     *
     * @param factory The {@link RedisConnectionFactory} provided by Spring Boot
     *                (e.g., Lettuce or Jedis) to manage the physical connection
     *                to the Redis server.
     * @return A configured {@link RedisCacheManager} for the application context.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Define the default behavior for all cache names
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // Prevents "Cache Bloat" by expiring entries after 10 minutes
                .entryTtl(Duration.ofMinutes(10))
                // Configures the Value Serializer
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
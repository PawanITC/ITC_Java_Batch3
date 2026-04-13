package com.itc.funkart.user.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * High-performance Kafka Producer configuration.
 * Configures serialization, idempotency, and batching strategies for user domain events.
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Defines the factory for creating Kafka Producers with optimized reliability settings.
     * * <ul>
     * <li><b>Idempotence:</b> Set to {@code true} to prevent duplicate events.</li>
     * <li><b>Acks:</b> Set to {@code all} for maximum data durability.</li>
     * <li><b>Interoperability:</b> Disables type info headers for cross-language compatibility.</li>
     * </ul>
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Connection & Serialization
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Interoperability: Strip Java-specific headers for non-JVM consumers
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Safety (The Banker Logic):
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 50);

        // Speed (Stall Tactic): Batching for efficiency
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // Identification
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "user-service-producer");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
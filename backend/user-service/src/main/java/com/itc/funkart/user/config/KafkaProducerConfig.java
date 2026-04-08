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

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // 1. CONNECTION: Where is the warehouse?
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 2. INTEROPERABILITY: Strip Java-specific headers so Python/Go consumers can read our JSON
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // 3. SAFETY (The Banker Logic):
        // "all" = Wait for Leader + all Followers to sync before confirming success
        // "true" = Prevents duplicate messages if a retry happens (Idempotence)
        // Retry forever if the network blips; we don't want to lose user events
        // Don't spam a crashed broker; wait 50ms before attempting to reconnect
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 50);

        // 4. Speed (Stall tactic) - wait before sending a batch and making sure we are efficient with how much we send at a time
        // Wait up to 5ms to see if more messages arrive to bundle them together
        // If we hit 16KB of data, ship the batch immediately (even if the 5ms isn't up)
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // 5. IDENTIFICATION: Helps us find this specific service in the Kafka logs
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "user-service-producer");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

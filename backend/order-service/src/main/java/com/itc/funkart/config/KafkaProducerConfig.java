package com.itc.funkart.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * <h2>KafkaProducerConfig</h2>
 * <p>
 * Configures the Kafka Producer to handle multiple Order Event types.
 * Optimized for JVM memory efficiency and cross-service compatibility.
 * </p>
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        // Pulls base config from application.yml (brokers, retries, etc.)
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);

        /*
         * STRATEGY: Cross-Language Compatibility vs. Java Type Info
         * Setting this to 'false' prevents Spring from adding the "__TypeId__" header.
         * This keeps the message payload clean for non-Java consumers (like Go or Node).
         */
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Standard reliability settings for Order Services
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Guarantee delivery
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 3000);

        // Instance identification for the Kafka Cluster
        String hostname = System.getenv().getOrDefault("HOSTNAME", "order-svc-prod");
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "order-producer-" + hostname);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
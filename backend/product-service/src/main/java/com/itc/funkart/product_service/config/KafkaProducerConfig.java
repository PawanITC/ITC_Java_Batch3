package com.itc.funkart.product_service.config;

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
 * <p>Configures the production environment for order domain events.</p>
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
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);

        // Strip Java-specific headers for cross-language compatibility
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Instance identification for monitoring
        String hostname = System.getenv().getOrDefault("HOSTNAME", "product-service-instance");
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "product-service-producer-" + hostname);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
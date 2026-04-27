package com.itc.funkart.user.config;

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
 * High-performance Kafka Producer configuration.
 * Configures serialization, idempotency, and batching strategies for user domain events.
 * This configuration leverages externalized properties from YAML while enforcing
 * specific interoperability and identification logic in code.
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * Constructor-based injection of {@link KafkaProperties}.
     * Spring Boot automatically populates this object with values from application.yaml.
     *
     * @param kafkaProperties The externalized Kafka properties.
     */
    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Defines the factory for creating Kafka Producers with optimized reliability settings.
     * This method builds properties from external configuration and applies custom overrides.
     * * <ul>
     * <li><b>Idempotence:</b> Enabled via YAML to prevent duplicate events.</li>
     * <li><b>Acks:</b> Set to {@code all} via YAML for maximum data durability.</li>
     * <li><b>Interoperability:</b> Disables type info headers in code for cross-language compatibility.</li>
     * <li><b>Identification:</b> Dynamically assigns a Client ID based on the system HOSTNAME.</li>
     * </ul>
     *
     * @return A configured {@link ProducerFactory} for String keys and Object values.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        // Fix: Use buildProducerProperties(null) to avoid deprecation in Spring Boot 3.2+
        // This merges all 'spring.kafka.producer' properties from your YAML.
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);

        // Interoperability: Strip Java-specific headers for non-JVM consumers (e.g., Python, Go)
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Identification: Dynamic Client ID for tracking specific instances in a cluster, Good for monitoring.
        String hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "user-service-producer-" + hostname);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Configures the {@link KafkaTemplate} used for high-level operations.
     * @return A {@link KafkaTemplate} initialized with the custom producer factory.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
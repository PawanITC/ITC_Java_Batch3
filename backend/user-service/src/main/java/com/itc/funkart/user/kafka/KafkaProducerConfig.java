package com.itc.funkart.user.kafka;

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
 * <h2>Kafka Producer Infrastructure</h2>
 *
 * <p>Configures the low-level serialization and network strategies for the user-service.
 * This configuration ensures that events produced by this service are compatible with
 * both JVM and non-JVM consumers by managing Type Information headers.</p>
 *
 * <p><b>Reliability Strategy:</b> Uses Banker-style persistence logic (acks=all, idempotence)
 * to ensure that user identity events are never lost or duplicated during transmission.</p>
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Builds the {@link ProducerFactory} using externalized YAML properties and hardcoded safety overrides.
     *
     * <ul>
     *   <li><b>MAX_BLOCK_MS:</b> Set to 5s to prevent JVM thread starvation during Kafka outages.</li>
     *   <li><b>ADD_TYPE_INFO_HEADERS:</b> Disabled to ensure interoperability with shared contracts.</li>
     *   <li><b>CLIENT_ID:</b> Dynamically assigned for cluster-wide observability.</li>
     * </ul>
     *
     * @return A thread-safe factory for creating Kafka Producers.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);

        // Interoperability: We disable type headers because we use a shared library
        // for DTOs. This forces consumers to rely on JSON structure rather than Java class names.
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Resilience: Prevent the producer from blocking the calling thread indefinitely
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

        // Observability: Hostname-based identification
        String hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "user-service-producer-" + hostname);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Provides the high-level {@link KafkaTemplate} for domain event publishing.
     *
     * @return A KafkaTemplate capable of sending any object as a JSON payload.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
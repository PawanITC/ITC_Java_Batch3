package com.itc.funkart.payment.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
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
 * Event-Driven Infrastructure for FunKart Payments.
 * <p>
 * This configuration enables the "Broadcast" of payment results to the rest of the
 * ecosystem (Order Service, Inventory, etc.). It converts Java objects into
 * JSON bytes for the Kafka Cluster.
 * </p>
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
     * Configures the blueprint for creating Kafka Producers.
     * * @return A factory capable of producing String-keyed JSON messages.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);

        /* * DEV NOTE: BOOTSTRAP_SERVERS
          configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
         * This is the "Entry Point" to our Kafka cluster (e.g., 'localhost:9092').
         * Without this, our service won't know where the message bus lives.
         */

        /* * SERIALIZATION:
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
         * Kafka only understands bytes. We use StringSerializer for the "Key"
         * (usually an Order ID) and JsonSerializer for the "Value" (the actual event).
         */

        /* * RELIABILITY: ACKS = ALL
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
         * This is the "Paranoid Mode." Setting ACKS to 'all' means the producer
         * waits for every replica in the cluster to confirm they received the
         * message before we consider it "Sent." Critical for financial data!
         */

        // Fix: Use buildProducerProperties(null) to avoid deprecation in Spring Boot 3.2+
        // This merges all 'spring.kafka.producer' properties from your YAML.

        /* TYPE_INFO_HEADERS: FALSE
         * By default, Spring Kafka adds the Java Class name (e.g., com.itc.payment.Event)
         * to the message header. If the 'Order Service' doesn't have that EXACT
         * package structure, it will crash. We set this to FALSE, this is to make our
         * JSON "Plain & Simple" so any service (Python, Go, JS) can read it.
         */
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Identification: Dynamic Client ID for tracking specific instances in a cluster
        String hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "payment-service-producer-" + hostname);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * The high-level API used by our code to send messages.
     * * @param producerFactory Pre-configured producer settings.
     * @return A KafkaTemplate for easy message dispatching.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
package com.itc.funkart.kafka.config;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * <h2>Order Service — Centralized Kafka Configuration</h2>
 * <p>
 * This class now handles both Topic Provisioning and the Listener Container Factory.
 * All serialization/deserialization logic is offloaded to application.yml.
 * </p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    // =========================================================================
    // 1. CONSUMER INFRASTRUCTURE
    // =========================================================================

    /**
     * Configures how @KafkaListener methods behave.
     * Spring automatically injects the ConsumerFactory built from your YAML.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // MANUAL_IMMEDIATE: Ensures we only commit the offset after the DB transaction
        // in OrderEventConsumer succeeds.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Set concurrency to match the number of partitions (3) defined below
        factory.setConcurrency(3);

        return factory;
    }

    // =========================================================================
    // 2. TOPIC PROVISIONING (Ownership: Only topics this service produces to)
    // =========================================================================

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersDlqTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
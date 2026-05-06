package com.itc.funkart.payment.kafka.config;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * <h2>Payment Service — Kafka Topic Provisioning</h2>
 *
 * <p>
 * Central place for all Kafka topics owned/used by Payment Service.
 * Topics are cluster-level resources and must NOT be duplicated across configs.
 * </p>
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Payment lifecycle events produced by Payment Service
     * and consumed by Order Service / Product Service.
     */
    @Bean
    public NewTopic paymentProcessTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENTS_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Dead-letter queue for failed payment processing events.
     */
    @Bean
    public NewTopic paymentDlqTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENTS_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
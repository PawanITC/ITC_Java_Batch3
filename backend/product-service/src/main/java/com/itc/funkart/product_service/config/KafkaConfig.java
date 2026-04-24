package com.itc.funkart.product_service.config;

import com.itc.funkart.product_service.constants.KafkaConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Infrastructure configuration for Kafka Messaging.
 * Defines the initial topics and partition counts required for the service.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name(KafkaConstants.TOPIC_PRODUCTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderTopics() {
        return TopicBuilder.name(KafkaConstants.TOPIC_ORDERS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
package com.itc.funkart.user.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * <h2>User Service — Kafka Infrastructure</h2>
 * <p>
 * This service "owns" the User and Auth topics.
 * Serialization and reliability settings are managed via application-dev.yml.
 * </p>
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic signupTopic() {
        return TopicBuilder.name(KafkaTopics.USER_SIGNUP)
                .partitions(3)
                .replicas(1)
                // Compact ensures we keep the latest state for every User ID
                .compact()
                .build();
    }

    @Bean
    public NewTopic loginTopic() {
        return TopicBuilder.name(KafkaTopics.AUTH_LOGIN)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return TopicBuilder.name(KafkaTopics.USER_UPDATED)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
}
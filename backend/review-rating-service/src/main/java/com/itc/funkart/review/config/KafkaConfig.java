package com.itc.funkart.review.config;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic reviewsTopic() {
        return TopicBuilder.name(KafkaTopics.REVIEWS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reviewsDlqTopic() {
        return TopicBuilder.name(KafkaTopics.REVIEWS_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

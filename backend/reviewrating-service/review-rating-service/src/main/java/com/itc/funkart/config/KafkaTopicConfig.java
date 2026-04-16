package com.itc.funkart.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    public static final String REVIEW_CREATED_TOPIC = "review-created";

    @Bean
    public NewTopic reviewCreatedTopic() {
        return new NewTopic(REVIEW_CREATED_TOPIC, 3, (short) 1);
    }
}

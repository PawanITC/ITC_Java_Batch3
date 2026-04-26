package com.itc.catalogueservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;
import org.springframework.kafka.retrytopic.RetryTopicSchedulerWrapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Profile("!test")
@Configuration
@EnableKafka
@EnableKafkaRetryTopic
public class KafkaConfig {

    @Bean(name = "kafkaRetryTopicSchedulerWrapper")
    public RetryTopicSchedulerWrapper kafkaRetryTopicSchedulerWrapper() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("kafka-retry-");
        scheduler.initialize();
        return new RetryTopicSchedulerWrapper(scheduler);
    }

}

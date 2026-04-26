package com.itc.catalogueservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@Profile("!test")
@Configuration
@EnableKafka
@EnableKafkaRetryTopic
public class KafkaConfig {

}

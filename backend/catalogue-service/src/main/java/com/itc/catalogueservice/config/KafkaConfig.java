package com.itc.catalogueservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;

@Profile("!test")
@Configuration
@EnableKafka
public class KafkaConfig {

}

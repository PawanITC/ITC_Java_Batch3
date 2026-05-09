package com.itc.funkart.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RatingAggregatorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RatingAggregatorServiceApplication.class, args);
    }
}

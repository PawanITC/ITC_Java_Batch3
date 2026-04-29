package com.itc.funkart.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * <h2>KafkaConsumerConfig</h2>
 * <p>
 * The "Hearing Aid" of the Payment Service. 👂
 * </p>
 * <p>
 * This configuration enables the service to listen for and process incoming distributed
 * events (like 'OrderCreated'). It defines how the service connects to the Kafka cluster,
 * joins a consumer group, and translates JSON bytes back into Java objects.
 * </p>
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    /**
     * The Engine that drives the {@code @KafkaListener} annotations.
     * <p>
     * This factory creates the "Containers" that run in the background, constantly
     * polling Kafka for new messages. It manages the threading model, allowing
     * multiple messages to be processed concurrently.
     * </p>
     *
     * @return A factory that Spring uses to power all listener methods in the application.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {

        /*
         * Minimal config. Spring Boot will automatically inject a ConsumerFactory
         * based on your YAML settings.
         */
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        /* * DEV NOTE: Error Handling
         * If we wanted to implement a 'Retry' or 'Dead Letter Topic' strategy,
         * we would configure it here on the factory level.
         */

        return factory;
    }
}
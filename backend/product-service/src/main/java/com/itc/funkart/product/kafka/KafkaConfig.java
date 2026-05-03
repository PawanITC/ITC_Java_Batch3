package com.itc.funkart.product.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Infrastructure configuration for Kafka Messaging.
 * Defines the initial topics and partition counts required for the service.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);

        // Senior Level: Dynamic Client ID for JMX monitoring
        String podName = System.getenv().getOrDefault("HOSTNAME", "local-dev");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "product-service-" + podName);

        // Best Practice: Disable type headers for cross-service compatibility
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCTS).partitions(3).build();
    }

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS).partitions(3).build();
    }
}
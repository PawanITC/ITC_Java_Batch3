package com.itc.funkart.product.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * <h2>Product Service Kafka Infrastructure</h2>
 *
 * <h3>Topic ownership principle:</h3>
 * <p>Each service provisions only the topics it <em>produces to</em>.
 * Consumers never provision producer topics — that avoids partition count conflicts
 * when both sides try to auto-create with different settings.</p>
 *
 * <p>Product Service produces to:</p>
 * <ul>
 *   <li>{@code PRODUCTS} — product catalog events (create, delete)</li>
 *   <li>{@code PRODUCT_INVENTORY} — stock level changes</li>
 *   <li>{@code ORDER_INITIATED} — checkout events sent downstream</li>
 * </ul>
 *
 * <h3>Fix:</h3>
 * <p>Was provisioning {@code ORDERS} ("orders.events.v1") — that topic belongs to
 * the Order Service. Removed it here. Added {@code ORDER_INITIATED} which is what
 * this service actually produces to.</p>
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productInventoryTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_INVENTORY)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderInitiatedTopic() {
        return TopicBuilder.name(KafkaTopics.CHECKOUT_INITIATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
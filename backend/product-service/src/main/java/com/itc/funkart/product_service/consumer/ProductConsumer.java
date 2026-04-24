package com.itc.funkart.product_service.consumer;

import com.itc.funkart.product_service.constants.KafkaConstants;
import com.itc.funkart.product_service.dto.events.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * <h2>ProductConsumer</h2>
 * <p>
 * Event listener responsible for processing messages from the Product topic.
 * </p>
 * <p>
 * This service acts as a subscriber in our event-driven architecture, allowing the
 * Product Service to react to catalog changes, stock updates, or external
 * synchronizations in real-time.
 * </p>
 */
@Service
@Slf4j
public class ProductConsumer {

    /**
     * Consumes {@link ProductEvent} messages from the Kafka cluster.
     * <p>
     * <b>Config:</b>
     * <ul>
     * <li><b>Topic:</b> {@value KafkaConstants#TOPIC_PRODUCTS}</li>
     * <li><b>Group ID:</b> products-group</li>
     * </ul>
     * </p>
     *
     * @param event The deserialized product event payload.
     */
    @KafkaListener(
            topics = KafkaConstants.TOPIC_PRODUCTS,
            groupId = "products-group"
    )
    public void consume(ProductEvent event) {
        log.info(">>>> [KAFKA CONSUMER] Event received from topic '{}': {}",
                KafkaConstants.TOPIC_PRODUCTS, event);

        // Business logic for event processing goes here
        // (e.g., updating a read-model, clearing a Redis cache, etc.)
    }
}
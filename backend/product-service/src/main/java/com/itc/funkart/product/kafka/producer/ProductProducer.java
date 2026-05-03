package com.itc.funkart.product.kafka.producer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.product.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * <h2>ProductProducer</h2>
 * <p>
 * Service responsible for broadcasting product state changes to the Kafka cluster.
 * Ensures downstream services (Search, Inventory) stay synchronized with the
 * Product Service "Source of Truth".
 * </p>
 *
 * @author Gemini Teacher
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends a product event to the global products' topic.
     *
     * @param event The payload containing change details (e.g., PRICE_UPDATE, NEW_PRODUCT).
     */
    public void sendMessage(ProductEvent event) {
        if (event == null) {
            log.warn(">>>> [KAFKA] Attempted to send a null ProductEvent. Aborting.");
            return;
        }

        String key = String.valueOf(event.id());
        log.debug(">>>> [KAFKA] Attempting dispatch for Product ID: {}", key);

        kafkaTemplate.send(KafkaTopics.PRODUCTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        handleSuccess(key, result);
                    } else {
                        handleFailure(key, ex);
                    }
                });
    }

    private void handleSuccess(String key, SendResult<String, Object> result) {
        log.info(">>>> [KAFKA SUCCESS] Event for ID {} sent to partition {} at offset {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, Throwable ex) {
        log.error(">>>> [KAFKA FAILURE] Sync error for Product {}. Error: {}",
                key, ex.getMessage());
    }
}
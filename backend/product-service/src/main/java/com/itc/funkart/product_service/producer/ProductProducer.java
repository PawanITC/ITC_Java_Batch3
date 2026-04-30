package com.itc.funkart.product_service.producer;

import com.itc.funkart.product_service.constants.KafkaConstants;
import com.itc.funkart.product_service.dto.events.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * <h2>ProductProducer</h2>
 * <p>
 * Event producer for catalog state synchronization.
 * </p>
 * <p>
 * Broadcasts product-related changes (CRUD operations) to the global event bus.
 * Downstream services such as Search (Elasticsearch/Solr) and Inventory use these
 * events to keep their local data projections in sync with the source of truth.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductProducer {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    /**
     * Broadcasts a {@link ProductEvent} to the system.
     *
     * @param event The event containing product metadata and the action type (CREATE/UPDATE/DELETE).
     */
    public void sendMessage(ProductEvent event) {
        String key = String.valueOf(event.id());
        log.info(">>>> [KAFKA PRODUCER] Dispatching Product Event: ID={}", key);

        kafkaTemplate.send(KafkaConstants.TOPIC_PRODUCTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        handleSuccess(key, result);
                    } else {
                        handleFailure(key, event, ex);
                    }
                });
    }

    private void handleSuccess(String key, SendResult<String, ProductEvent> result) {
        log.info(">>>> [KAFKA SUCCESS] Product event {} confirmed at partition {} with offset {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, ProductEvent event, Throwable ex) {
        log.error(">>>> [KAFKA FAILURE] Dispatch failed for Product {}: {}", key, ex.getMessage());
        // TODO: Save to 'failed_events' table for dead-letter processing
    }
}
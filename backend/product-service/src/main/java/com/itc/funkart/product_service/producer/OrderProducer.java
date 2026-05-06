package com.itc.funkart.product_service.producer;

import com.itc.funkart.product_service.constants.KafkaConstants;
import com.itc.funkart.product_service.dto.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * <h2>OrderProducer</h2>
 * <p>
 * Asynchronous event producer responsible for dispatching order placement events.
 * </p>
 * <p>
 * This service facilitates the hand-off from the shopping cart to the Order microservice.
 * It uses the User ID as the Kafka message key to ensure that all orders from a specific
 * user are processed in chronological order by downstream consumers.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    /**
     * Dispatches an {@link OrderEvent} to the Kafka cluster.
     * <p>
     * This method is non-blocking. It utilizes {@code CompletableFuture} to handle
     * acknowledgement (ACK) from the Kafka brokers.
     * </p>
     *
     * @param event The event payload containing cart details and user metadata.
     */
    public void sendOrderEvent(OrderEvent event) {
        String key = String.valueOf(event.userId());
        log.info(">>>> [KAFKA PRODUCER] Dispatching Order Event for User: {}", key);

        kafkaTemplate.send(KafkaConstants.TOPIC_ORDERS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        handleSuccess(key, result);
                    } else {
                        handleFailure(key, event, ex);
                    }
                });
    }

    private void handleSuccess(String key, SendResult<String, OrderEvent> result) {
        log.info(">>>> [KAFKA SUCCESS] Order event {} confirmed at partition {} with offset {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, OrderEvent event, Throwable ex) {
        log.error(">>>> [KAFKA FAILURE] Critical failure dispatching Order {}: {}", key, ex.getMessage());
        // TODO: Persist to 'failed_events' table for manual reconciliation or automated retry
    }
}
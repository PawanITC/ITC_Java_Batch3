package com.itc.funkart.product.kafka.producer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * <h2>OrderProducer</h2>
 * <p>
 * Asynchronous event producer responsible for dispatching order placement events.
 * Uses a standardized {@link KafkaTemplate} with {@code Object} value type to
 * satisfy Spring's Dependency Injection requirements.
 * </p>
 *
 * @author Gemini Teacher
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Dispatches an {@link OrderInitiatedEvent} to the Kafka cluster.
     * <p>
     * Uses the User ID as the message key to ensure partition affinity (all orders
     * from one user go to the same partition, preserving chronological order).
     * </p>
     *
     * @param event The event payload containing cart details.
     */
    public void sendOrderEvent(OrderInitiatedEvent event) {
        if (event == null || event.userId() == null) {
            log.error(">>>> [KAFKA] Aborting: OrderEvent or UserId is null.");
            return;
        }

        String key = String.valueOf(event.userId());
        log.info(">>>> [KAFKA PRODUCER] Dispatching Order Event for User: {}", key);

        // We pass the OrderEvent record directly; the Template handles the Object-level abstraction
        kafkaTemplate.send(KafkaTopics.ORDERS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        handleSuccess(key, result);
                    } else {
                        handleFailure(key, ex);
                    }
                });
    }

    private void handleSuccess(String key, SendResult<String, Object> result) {
        log.info(">>>> [KAFKA SUCCESS] Order event {} confirmed at partition {} with offset {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, Throwable ex) {
        log.error(">>>> [KAFKA FAILURE] Critical failure dispatching Order {}: {}", key, ex.getMessage());
        // TODO: Implement Transactional Outbox pattern here for Senior level reliability
    }
}
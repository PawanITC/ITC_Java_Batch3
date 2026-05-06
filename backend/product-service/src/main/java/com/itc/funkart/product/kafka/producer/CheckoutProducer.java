package com.itc.funkart.product.kafka.producer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * <h2>OrderProducer (Product Service)</h2>
 *
 * <p>Dispatches an {@link CheckoutInitiatedEvent} when a user completes checkout
 * in the cart. This is Stage 1 of the order saga — it signals the Order Service
 * to create the DB record and forward to Payment.</p>
 *
 * <h3>Fix applied:</h3>
 * <p>Previous version published to {@code KafkaTopics.ORDERS} ("orders.events.v1").
 * That topic is produced by the <em>Order Service</em> and consumed by <em>Payment Service</em>.
 * This producer must publish to {@code KafkaTopics.ORDER_INITIATED} ("orders.events.initiated.v1")
 * which is the topic the Order Service consumer ({@code OrderEventConsumer}) actually listens on.
 * Publishing to the wrong topic meant Order Service never received checkout events.</p>
 *
 * <h3>Partition strategy:</h3>
 * <p>Uses the customer ID as the message key to ensure all events for one user
 * land on the same partition, preserving chronological order per customer.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> sendCheckoutEvent(CheckoutInitiatedEvent event) {

        if (event == null || event.customerId() == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid checkout event"));
        }

        String key = String.valueOf(event.customerId());

        return kafkaTemplate.send(KafkaTopics.CHECKOUT_INITIATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ CHECKOUT sent partition={} offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ CHECKOUT failed for customer={}", key, ex);
                    }
                });
    }
}
package com.itc.funkart.kafka.producer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * <h2>Order Event Producer</h2>
 * <p>
 * Broadcasts order lifecycle events downstream. Publishes to {@code ORDERS}
 * ("orders.events.v1") which the Payment Service consumes to execute the Stripe charge.
 * </p>
 *
 * <h3>Bug fix — wrong event type published to Payment:</h3>
 * <p>The old {@code publishOrderCreated(OrderInitiatedEvent)} was forwarding the raw
 * checkout event from Product Service straight to Payment. Payment Service's
 * {@code onOrderCreated} handler expects {@code orderId}, {@code paymentIntentId},
 * and {@code paymentMethodId} — none of which are reliable on {@code OrderInitiatedEvent}
 * at that stage.</p>
 *
 * <p>Corrected to publish an {@link OrderEvent} built by {@code OrderMapper.toEvent()},
 * which carries the persisted {@code orderId} assigned by the DB. Payment Service
 * uses the {@code orderId} to look up the pre-warmed Stripe intent from its own record
 * (the fallback path in {@code OrderCreatedConsumer.processStripeFulfillment}).</p>
 *
 * <h3>Topic ownership:</h3>
 * <p>Order Service provisions and produces to {@code ORDERS} and {@code ORDERS_DLQ}.
 * Product Service no longer provisions this topic.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a confirmed order event to the Payment Service.
     * <p>
     * Call this after the Order entity is persisted so {@code orderId} is a real DB key.
     * Payment Service uses this to look up the pre-warmed Stripe Intent and capture funds.
     * </p>
     *
     * @param event Fully populated {@link OrderEvent} from {@code OrderMapper.toEvent()}.
     */
    public void publishOrderEvent(OrderEvent event) {
        log.debug("📤 Preparing OrderEvent [Type: {}] for Order ID: {}",
                event.eventType(), event.orderId());
        send(KafkaTopics.ORDERS, event.orderId(), event);
    }


    /**
     * Publishes an order cancellation event.
     * <p>
     * Consumed by Payment Service (to trigger a refund if already charged)
     * and Inventory Service (to release reserved stock).
     * </p>
     *
     * @param event Built by {@code OrderMapper.toCancelledEvent()}.
     */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.debug("🚫 Preparing OrderCancelledEvent for Order ID: {}", event.orderId());
        send(KafkaTopics.ORDERS, event.orderId(), event);
    }

    /**
     * Internal dispatch — publishes to the given topic with orderId as partition key.
     */
    private void send(String topic, Long key, Object payload) {
        String stringKey = key != null ? key.toString() : "UNKNOWN";

        kafkaTemplate.send(topic, stringKey, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("❌ [KAFKA NACK] Topic: {} | Key: {} | Reason: {}",
                                topic, stringKey, ex.getMessage());
                        // TODO: Publish to DLQ (KafkaTopics.ORDERS_DLQ) for retry
                    } else {
                        log.info("✅ [KAFKA ACK] Topic: {} | Key: {} | Partition: {} | Offset: {}",
                                topic, stringKey,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
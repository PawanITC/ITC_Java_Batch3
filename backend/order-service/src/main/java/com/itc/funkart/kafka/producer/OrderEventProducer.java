package com.itc.funkart.kafka.producer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * <h2>Order Event Producer</h2>
 * <p>
 * This service acts as the "Megaphone" of the Order Microservice. It broadcasts
 * order lifecycle changes (creation, cancellation, etc.) to the rest of the
 * FunKart ecosystem via Apache Kafka.
 * </p>
 *
 * <h3>Key Concepts for the Team:</h3>
 * <ul>
 *   <li><b>Partition Affinity:</b> We use the {@code orderId} as the Kafka Message Key.
 *   This ensures all events for the same order stay in the correct sequence.</li>
 *   <li><b>Non-Blocking I/O:</b> Instead of making the JVM wait for Kafka,
 *   we use callbacks to log results. This keeps the application fast.</li>
 *   <li><b>Immutability:</b> Using Java Records for events ensures thread-safety
 *   when processing data across the JVM.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String TOPIC = KafkaTopics.ORDERS;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Broadcasts the initial order creation.
     */
    public void publishOrderCreated(OrderInitiatedEvent event) {
        log.debug("🛠️ Preparing OrderInitiatedEvent for ID: {}", event.orderId());
        send(event.orderId(), event);
    }

    /**
     * Broadcasts a full order state change (including items).
     */
    public void publishOrderEvent(OrderEvent event) {
        log.debug("🛠️ Preparing full OrderEvent [Type: {}] for ID: {}",
                event.eventType(), event.orderId());
        send(event.orderId(), event);
    }

    /**
     * Broadcasts an order cancellation.
     */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.debug("🚫 Preparing OrderCancelledEvent for ID: {}", event.orderId());
        send(event.orderId(), event);
    }

    /**
     * <h3>Internal Dispatch Helper</h3>
     * <p>
     * Sends the payload to Kafka and attaches a callback to handle the
     * success or failure of the delivery.
     * </p>
     */
    private void send(Long key, Object payload) {
        String stringKey = key != null ? key.toString() : "UNKNOWN";

        kafkaTemplate.send(TOPIC, stringKey, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("❌ Kafka NACK [ID: {}]. Reason: {}",
                                stringKey, ex.getMessage());
                    } else {
                        log.info("✅ Kafka ACK [ID: {} | Partition: {} | Offset: {}]",
                                stringKey,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
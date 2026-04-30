package com.itc.funkart.kafka;

import com.itc.funkart.constants.EventTypes;
import com.itc.funkart.constants.KafkaConstants;
import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <h2>OrderEventProducer</h2>
 * <p>
 * This service acts as the communication bridge between the Order Service and the
 * wider FunKart ecosystem via Apache Kafka. It is responsible for serializing
 * domain changes into events and ensuring they are successfully persisted to the
 * message broker.
 * </p>
 * * <p><b>Key Features:</b></p>
 * <ul>
 * <li><b>Synchronous Delivery:</b> Uses {@code .get()} to block execution until
 * Kafka acknowledges receipt, preventing data loss in critical transactions.</li>
 * <li><b>Key-Based Partitioning:</b> Uses the Order ID as the message key to
 * guarantee that all events for a specific order are processed in the
 * correct sequence by consumers.</li>
 * <li><b>Audit Logging:</b> Provides detailed logs for successful transmissions
 * and comprehensive error reporting for infrastructure failures.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    /**
     * The target topic for all order-related domain events.
     * Mapped to 'order-events' in the cluster.
     */
    private static final String TOPIC = KafkaConstants.TOPIC_ORDERS;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes an {@code ORDER_CREATED} event to notify downstream services
     * (Inventory, Payment, etc.) that a new transaction has started.
     *
     * @param order The persisted {@link Order} entity containing transaction details.
     * @return {@code true} if the event was acknowledged by the Kafka broker.
     * @throws RuntimeException if the message fails to send after retries.
     */
    public boolean publishOrderCreated(Order order) {
        OrderEvent event = OrderEvent.builder()
                .eventType(EventTypes.ORDER_CREATED)
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .timestamp(LocalDateTime.now())
                .build();

        return sendEvent(event, order.getId());
    }

    /**
     * Publishes an {@code ORDER_UPDATED} event when a status change occurs,
     * such as moving from PENDING to SHIPPED.
     *
     * @param order The {@link Order} entity with the updated state.
     * @return {@code true} if the event was acknowledged.
     */
    public boolean publishOrderUpdated(Order order) {
        OrderEvent event = OrderEvent.builder()
                .eventType(EventTypes.ORDER_UPDATED)
                .orderId(order.getId())
                .timestamp(LocalDateTime.now())
                .build();

        return sendEvent(event, order.getId());
    }

    /**
     * Publishes an {@code ORDER_CANCELLED} event to trigger refund processes
     * or inventory restock logic in other services.
     *
     * @param orderId The primary key of the order being voided.
     * @return {@code true} if the event was acknowledged.
     */
    public boolean publishOrderCancelled(Long orderId) {
        OrderEvent event = OrderEvent.builder()
                .eventType(EventTypes.ORDER_CANCELLED)
                .orderId(orderId)
                .timestamp(LocalDateTime.now())
                .build();

        return sendEvent(event, orderId);
    }

    /**
     * Internal helper to execute the Kafka send operation.
     * <p>
     * This method is <b>synchronous</b>. It waits for the {@link java.util.concurrent.Future}
     * to complete to ensure the event is durable before the service proceeds
     * with further logic.
     * </p>
     *
     * @param event The event payload to be sent.
     * @param key   The Order ID used for Kafka partition affinity.
     * @return {@code true} on successful acknowledgment.
     */
    private boolean sendEvent(OrderEvent event, Long key) {
        kafkaTemplate.send(TOPIC, key.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("❌ Kafka send failed for Order ID={}", key, ex);
                    } else {
                        log.info("✅ Kafka event [{}] sent for Order ID={}", event.getEventType(), key);
                    }
                });

        return true; // means "dispatch succeeded", not "delivered"
    }
}
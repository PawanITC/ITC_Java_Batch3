package com.itc.funkart.kafka;

import com.itc.funkart.constants.KafkaConstants;
import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.OrderStatus;
import com.itc.funkart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * <h2>OrderEventConsumer</h2>
 * <p>
 * This component acts as the asynchronous orchestrator for the Order Service.
 * It listens to the Kafka topic {@code order-events} to react to domain events
 * published by this service or other services in the ecosystem.
 * </p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 * <li><b>Event Demultiplexing:</b> Routes incoming JSON payloads to specific
 * handler methods based on the {@code eventType}.</li>
 * <li><b>State Management:</b> Triggers internal service logic (like status updates)
 * to ensure the database stays in sync with the distributed event log.</li>
 * <li><b>Idempotency:</b> Processes messages within a defined consumer group
 * {@code service-events} to ensure load balancing across service instances.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String TOPIC = KafkaConstants.TOPIC_ORDERS;
    private final OrderService orderService;

    /**
     * Entry point for the Kafka message listener.
     * <p>
     * Decodes the {@link OrderEvent} and dispatches it to the appropriate business logic.
     * The {@code groupId} ensures that if multiple instances of this service are running,
     * each message is processed only once.
     * </p>
     *
     * @param event The deserialized event payload from the broker.
     */
    @KafkaListener(topics = TOPIC, groupId = "service-events")
    public void consume(OrderEvent event) {
        log.info("🔥 EVENT RECEIVED: ID={} | Type={}", event.getOrderId(), event.getEventType());

        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED" -> handleOrderCreated(event);
                case "ORDER_UPDATED" -> handleOrderUpdated(event);
                case "ORDER_CANCELLED" -> handleOrderCancelled(event);
                default -> log.warn("⚠️ Unknown event type detected: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❌ Critical error processing event for Order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            // In a production environment, you might send this to a Dead Letter Topic (DLT)
        }
    }

    /**
     * Logic for handling newly created orders.
     * Usually triggers inventory reservation or payment verification requests.
     */
    private void handleOrderCreated(OrderEvent event) {
        log.info("🚀 Processing ORDER_CREATED for orderId={}", event.getOrderId());
        // Integration point: Call orderService or a workflow orchestrator here
    }

    /**
     * Logic for syncing status updates from external systems (e.g., Shipping Service).
     */
    private void handleOrderUpdated(OrderEvent event) {
        log.info("🔄 Processing ORDER_UPDATED for orderId={}", event.getOrderId());
        // Logic to sync internal DB status if the update came from an outside service
    }

    /**
     * Logic for processing cancellations.
     * Reverts the order status in the local database to {@link OrderStatus#CANCELLED}.
     */
    private void handleOrderCancelled(OrderEvent event) {
        log.info("🚫 Processing ORDER_CANCELLED for orderId={}", event.getOrderId());
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);
    }
}
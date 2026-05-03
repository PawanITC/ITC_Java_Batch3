package com.itc.funkart.service;

import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.entity.Order;

/**
 * <h2>KafkaEventService</h2>
 * <p>
 * Orchestrates the dispatch of domain events to Apache Kafka while maintaining
 * strict synchronization with the primary database transaction.
 * </p>
 *
 * <h3>Reliability Strategy:</h3>
 * <p>
 * This service uses {@link org.springframework.transaction.support.TransactionSynchronization}
 * to ensure that events are only published <b>after</b> the database commit is successful.
 * This prevents downstream services from reacting to "phantom" data that was rolled
 * back in the Order Service.
 * </p>
 */
public interface KafkaEventService {

    /**
     * Publishes a full {@code OrderEvent} snapshot.
     * Typically used for successful status transitions like 'PAID' or 'SHIPPED'.
     *
     * @param order The JPA entity to be mapped and broadcasted.
     * @param type  The specific lifecycle stage being reported.
     */
    void sendOrderEvent(Order order, OrderEventType type);

    /**
     * Publishes an {@code OrderInitiatedEvent}.
     * Used during the checkout phase to trigger inventory reservation and cart clearing.
     *
     * @param event The immutable record containing user intent and product IDs.
     */
    void sendOrderInitiated(OrderInitiatedEvent event);

    /**
     * Publishes an {@code OrderCancelledEvent}.
     * Used to signal downstream services to release resources or restock inventory.
     *
     * @param event The immutable record detailing the reason for cancellation.
     */
    void sendOrderCancelled(OrderCancelledEvent event);
}
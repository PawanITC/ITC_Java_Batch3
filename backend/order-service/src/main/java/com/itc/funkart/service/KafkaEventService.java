package com.itc.funkart.service;

import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.entity.Order;

/**
 * <h2>KafkaEventService</h2>
 * <p>
 * Transaction-aware Kafka dispatch contract for the Order Service.
 * All events are published via afterCommit hooks — Kafka never fires
 * if the DB transaction rolls back.
 * </p>
 */
public interface KafkaEventService {

    /**
     * Publishes a full order state event post-commit.
     * Currently guards against non-ORDER_INITIATED types until downstream
     * consumers are implemented for those event types.
     */
    void sendOrderEvent(Order order, OrderEventType eventType);

    /**
     * Maps the persisted {@link Order} entity into an {@code OrderEvent} and
     * dispatches it to the {@code ORDERS} topic post-commit.
     * <p>
     * Call this after the Order is saved so it has a real DB-assigned {@code id}.
     * Payment Service uses that {@code id} to look up the pre-warmed Stripe Intent.
     * </p>
     *
     * @param order Must have a non-null {@code id} (already saved to DB).
     */
    void sendOrderCreated(Order order);

    /**
     * Publishes an order cancellation event post-commit.
     */
    void sendOrderCancelled(OrderCancelledEvent event);
}
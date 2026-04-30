package com.itc.funkart.service;

import com.itc.funkart.entity.Order;

/**
 * <h2>KafkaEventService</h2>
 * <p>Handles the abstraction of the messaging layer.
 * Decouples business transactions from event broadcasting.</p>
 */
public interface KafkaEventService {
    /**
     * Publishes an event to notify the ecosystem of a new or updated order.
     *
     * @param order The order entity to be broadcasted.
     * @return true if published successfully, false if queued for retry.
     */
    boolean sendOrderEvent(Order order);
}
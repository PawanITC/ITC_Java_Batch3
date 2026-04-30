package com.itc.funkart.constants;

/**
 * <h2>KafkaConstants</h2>
 * <p>
 * Centralized registry for Kafka topic names. This ensures that the OrderEventProducer
 * and OrderEventConsumer are always looking at the same "channel."
 * </p>
 */
public final class KafkaConstants {

    /**
     * Primary topic for order lifecycle events (CREATED, UPDATED, CANCELLED).
     * Replaces the old 'order-events3' with a clean, version-neutral name.
     */
    public static final String TOPIC_ORDERS = "order-events";

    /**
     * Optional: Topic for listening to inventory confirmations (if applicable later).
     */
    public static final String TOPIC_INVENTORY = "inventory-events";

    private KafkaConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
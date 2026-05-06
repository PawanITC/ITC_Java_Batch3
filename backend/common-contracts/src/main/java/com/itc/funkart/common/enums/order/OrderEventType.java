package com.itc.funkart.common.enums.order;

/**
 * <h2>Kafka Event Classifiers</h2>
 * Labels used in the headers or payloads of Kafka messages to define
 * the nature of an Order-related event.
 */
public enum OrderEventType {
    /** Emitted when a new order is placed but not yet paid. */
    ORDER_INITIATED,

    /**
     * ✅ CRITICAL EVENT
     * Emitted ONLY after the order is successfully persisted in the database.
     * This guarantees orderId is NOT NULL.
     */
    CREATED,

    /** Emitted when payment is successfully captured (PAID). */
    PAYMENT_SUCCESS,

    /** Emitted when payment fails or is rejected. */
    PAYMENT_FAILED,

    /** NEW: Emitted when items are dispatched (SHIPPED). */
    ORDER_SHIPPED,

    /** NEW: Emitted when customer receives package (DELIVERED). */
    ORDER_DELIVERED,

    /** Emitted when an order is voided (CANCELLED). */
    ORDER_CANCELLED,

    /** NEW: Emitted when funds are returned (REFUNDED). */
    ORDER_REFUNDED,

    /** NEW: Emitted when funds are confirmed (CONFIRMED). */
    ORDER_CONFIRMED
}
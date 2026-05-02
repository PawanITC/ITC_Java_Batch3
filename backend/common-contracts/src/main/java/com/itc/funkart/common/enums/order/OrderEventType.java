package com.itc.funkart.common.enums.order;

/**
 * <h2>Kafka Event Classifiers</h2>
 * Labels used in the headers or payloads of Kafka messages to define
 * the nature of an Order-related event.
 */
public enum OrderEventType {
    /** Emitted when a new order is placed but not yet paid. */
    ORDER_INITIATED,

    /** Emitted when payment is successfully captured. */
    PAYMENT_SUCCESS,

    /** Emitted when payment fails or is rejected. */
    PAYMENT_FAILED,

    /** Emitted when an order is manually or systematically voided. */
    ORDER_CANCELLED
}
package com.itc.funkart.product_service.enums;

/**
 * Represents the lifecycle stages of an order for Kafka event processing.
 * Used to track the state transitions from creation to completion or failure.
 */
public enum OrderEventType {
    /**
     * Order has been placed but not yet processed.
     */
    ORDER_CREATED,
    /**
     * Order was cancelled by the user or system.
     */
    ORDER_CANCELLED,
    /**
     * Order has been successfully paid and processed.
     */
    ORDER_COMPLETED,
    /**
     * Order processing failed (e.g., payment declined or stock unavailable).
     */
    ORDER_FAILED
}
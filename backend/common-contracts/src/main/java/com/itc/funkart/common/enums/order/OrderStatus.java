package com.itc.funkart.common.enums.order;

import lombok.Getter;

/**
 * <h2>Order Lifecycle Status</h2>
 * <p>
 * Defines the chronological stages of a transaction. This Enum is the source of truth
 * for state transitions across the Funkart ecosystem.
 * </p>
 *
 * <p>
 * From a JVM perspective, using an Enum ensures that state comparisons are handled
 * via memory address identity, which is faster than character-based String comparisons.
 * </p>
 */
@Getter
public enum OrderStatus {

    /**
     * Order created; awaiting payment confirmation.
     */
    PENDING(false),

    /**
     * Payment verified; order is ready for fulfillment.
     */
    PAID(false),

    /**
     * Items dispatched from the warehouse.
     */
    SHIPPED(false),

    /**
     * Customer has received the package.
     */
    DELIVERED(true),

    /**
     * Order voided before fulfillment.
     */
    CANCELLED(true),

    /**
     * Transaction reversed and funds returned.
     */
    REFUNDED(true);

    /**
     * Indicates if the status is a terminal state in the state machine.
     */
    private final boolean isFinal;

    OrderStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * Determines if the order is eligible for cancellation.
     *
     * @return true if the order is in a pre-fulfillment state.
     */
    public boolean isCancellable() {
        return this == PENDING || this == PAID;
    }
}
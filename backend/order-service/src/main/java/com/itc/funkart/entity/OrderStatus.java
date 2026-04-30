package com.itc.funkart.entity;

import lombok.Getter;

/**
 * <h2>Order Lifecycle Status</h2>
 * <p>
 * This enumeration defines the various stages an order transitions through,
 * from initial creation to final fulfillment or termination.
 * </p>
 * * @author Abbas Gure
 *
 * @version 1.1
 */
@Getter
public enum OrderStatus {

    /**
     * The order has been created, but payment has not yet been verified.
     */
    PENDING(false),

    /**
     * Payment has been successfully processed and verified.
     */
    CONFIRMED(false),

    /**
     * The order has been packed and handed over to the logistics provider.
     */
    SHIPPED(false),

    /**
     * The logistics provider has confirmed successful delivery to the customer.
     */
    DELIVERED(true),

    /**
     * The order was terminated before fulfillment.
     */
    CANCELLED(true),

    /**
     * The order was delivered, but the customer returned the items and funds reversed.
     */
    REFUNDED(true);

    /**
     * Indicates if the status represents a final state where no further
     * standard transitions (like shipping or delivery) can occur.
     */
    private final boolean isFinal;

    OrderStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * Checks if the order can still be cancelled.
     * <p>Orders generally cannot be cancelled once they have been SHIPPED.</p>
     * * @return true if the order is still in a cancellable state.
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }
}
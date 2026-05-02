package com.itc.funkart.common.enums.order;

/**
 * <h2>Order Lifecycle States</h2>
 * Represents the chronological progression of a customer's order.
 */
public enum OrderStatus {
    /** Initial state: Order created but payment not confirmed. */
    PENDING,
    /** Payment successfully processed. */
    PAID,
    /** Items have left the warehouse. */
    SHIPPED,
    /** Order terminated by user or system. */
    CANCELLED,
    /** Funds returned to the customer. */
    REFUNDED
}
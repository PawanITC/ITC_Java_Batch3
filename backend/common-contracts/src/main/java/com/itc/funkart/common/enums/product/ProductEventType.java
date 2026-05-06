package com.itc.funkart.common.enums.product;

/**
 * Defines the types of operations performed on products for event-driven synchronization.
 * These events are typically published to Kafka topics to update search indexes or caches.
 */
public enum ProductEventType {
    /**
     * A new product was added to the catalog.
     */
    CREATE,
    /**
     * Existing product details or inventory were modified.
     */
    UPDATE,
    /**
     * A product was removed from the catalog.
     */
    DELETE
}
